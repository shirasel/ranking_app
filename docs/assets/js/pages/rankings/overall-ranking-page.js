(function () {
  "use strict";

  class OverallRankingPage {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.window = dependencies.window;
      this.allEntries = [];
      this.container = this.app.qs("[data-overall-ranking]");
      this.input = this.app.qs("[data-ranking-search]");
      this.periodButtons = this.app.qsa("[data-period-button]");
      this.viewButtons = this.app.qsa("[data-ranking-view]");
      this.historyButton = this.app.qs("[data-history-button]");
      this.historyPicker = this.app.qs("[data-history-picker]");
      this.historySelect = this.app.qs("[data-overall-history-select]");
      this.labels = dependencies.labels || new window.YTRankPages.OverallRankingViewLabels();
      this.entryFilter = dependencies.entryFilter || new window.YTRankPages.RankingEntryFilter();
      this.historySelector = dependencies.historySelector || new window.YTRankPages.OverallHistorySelector({
        app: this.app,
        select: this.historySelect
      });
      this.urlState = dependencies.urlState || new window.YTRankPages.OverallRankingUrlState({
        window: this.window,
        selectedHistoryItem: this.historySelector.selectedItem.bind(this.historySelector)
      });
      this.emptyStateRenderer = dependencies.emptyStateRenderer || new window.YTRankPages.OverallEmptyStateRenderer({
        app: this.app,
        container: this.container,
        labels: this.labels
      });
      this.initialView = this.urlState.initialView();
      this.initialQuery = this.urlState.initialQuery();
      this.initialDate = this.urlState.initialDate();
      this.currentPath = "latest/overall.json";
      this.currentView = "overall";
      this.historyItems = [];
    }

    init() {
      this.bindPeriodButtons();
      this.bindHistoryControls();
      if (this.input) this.input.value = this.initialQuery;
      this.loadInitialView();
      this.bindSearch();
    }

    bindPeriodButtons() {
      var self = this;
      this.periodButtons.forEach(function (button) {
        button.addEventListener("click", function () {
          self.loadOverall(
            button.getAttribute("data-period-button"),
            button.getAttribute("data-ranking-view") || "overall",
            true
          );
        });
      });
    }

    bindHistoryControls() {
      var self = this;
      if (this.historyButton) {
        this.historyButton.addEventListener("click", function () {
          if (self.historyItems.length && self.historySelect && self.historySelect.value) {
            self.loadOverallHistory(self.historySelect.value, true);
            return;
          }
          self.loadOverallHistoryIndex(true);
        });
      }

      if (this.historySelect) {
        this.historySelect.addEventListener("change", function () {
          self.loadOverallHistory(self.historySelect.value, true);
        });
      }
    }

    bindSearch() {
      var self = this;
      if (!this.input) return;
      this.input.addEventListener("input", function () {
        self.updateViewUrl(self.currentView, self.input.value.trim());
        self.renderFilteredEntries();
      });
    }

    loadInitialView() {
      if (this.initialView === "history") {
        this.loadOverallHistoryIndex(false);
        return;
      }

      var initialButton = this.periodButtons.find(function (button) {
        return button.getAttribute("data-ranking-view") === this.initialView;
      }, this) || this.periodButtons[0];
      this.loadOverall(
        initialButton.getAttribute("data-period-button"),
        initialButton.getAttribute("data-ranking-view") || "overall",
        false
      );
    }

    renderOverallEntries(entries, query) {
      var app = this.app;
      app.showState("overall", "");
      if (entries.length) {
        app.renderRankingList(this.container, entries);
        return;
      }

      this.emptyStateRenderer.render(this.currentPath, this.currentView, query);
    }

    setActiveButton(view) {
      this.viewButtons.forEach(function (item) {
        item.classList.toggle("active", item.getAttribute("data-ranking-view") === view);
      });
    }

    selectedHistoryItem() {
      return this.historySelector.selectedItem();
    }

    updateViewUrl(view, query) {
      this.urlState.update(view, query);
    }

    toggleHistoryPicker(view) {
      if (!this.historyPicker) return;
      this.historyPicker.hidden = view !== "history";
    }

    applyViewState(view) {
      this.currentView = view || "overall";
      this.setActiveButton(this.currentView);
      this.toggleHistoryPicker(this.currentView);
      this.app.setText("[data-ranking-eyebrow]", this.labels.eyebrowFor(this.currentView));
      this.app.setText("[data-ranking-title]", this.labels.titleFor(this.currentView));
    }

    loadOverall(path, view, shouldUpdateUrl) {
      var self = this;
      this.currentPath = path;
      this.applyViewState(view);
      if (shouldUpdateUrl) this.updateViewUrl(this.currentView, this.input ? this.input.value.trim() : "");
      this.app.showState("overall", "ランキングJSONを読み込んでいます。");
      this.app.loadJson(path)
        .then(function (document) {
          self.app.setText("[data-updated-at]", "最終更新 " + self.app.formatDateTime(document.generatedAt));
          self.allEntries = document.ranking || [];
          self.renderFilteredEntries();
        })
        .catch(function () {
          self.app.showState("overall", "ランキングデータを読み込めませんでした。");
          self.app.clear(self.container);
        });
    }

    loadOverallHistory(path, shouldUpdateUrl) {
      var self = this;
      this.currentPath = path;
      this.applyViewState("history");
      if (shouldUpdateUrl) this.updateViewUrl(this.currentView, this.input ? this.input.value.trim() : "");
      this.app.showState("overall", "過去日ランキングJSONを読み込んでいます。");
      this.app.loadJson(path)
        .then(function (document) {
          self.app.setText("[data-updated-at]", "生成 " + self.app.formatDateTime(document.generatedAt));
          self.allEntries = document.ranking || [];
          self.renderFilteredEntries();
        })
        .catch(function () {
          self.app.showState("overall", "過去日ランキングを読み込めませんでした。");
          self.app.clear(self.container);
        });
    }

    loadOverallHistoryIndex(shouldUpdateUrl) {
      var self = this;
      this.applyViewState("history");
      this.app.showState("overall", "履歴インデックスを読み込んでいます。");
      this.app.loadJson("latest/history-index.json")
        .then(function (index) {
          self.historyItems = (index.items || []).slice().reverse();
          if (!self.historyItems.length) {
            self.app.showState("overall", "過去日ランキングはまだありません。");
            self.app.clear(self.container);
            return;
          }

          self.historySelector.setItems(self.historyItems);
          var selectedItem = self.historySelector.selectInitial(self.initialDate);
          self.loadOverallHistory(selectedItem.path, shouldUpdateUrl);
        })
        .catch(function () {
          self.app.showState("overall", "履歴インデックスを読み込めませんでした。");
          self.app.clear(self.container);
        });
    }

    renderFilteredEntries() {
      var query = this.input ? this.input.value.trim().toLowerCase() : "";
      var filtered = this.entryFilter.filter(this.allEntries, query);
      this.renderOverallEntries(filtered, query);
    }
  }

  window.YTRankPages = window.YTRankPages || {};
  window.YTRankPages.OverallRankingPage = OverallRankingPage;
})();
