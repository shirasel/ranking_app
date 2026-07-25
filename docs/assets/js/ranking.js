(function () {
  "use strict";

  class RankingPageRouter {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.document = dependencies.document;
      this.window = dependencies.window;
      this.pages = {
        home: HomeRankingPage,
        overall: OverallRankingPage,
        genre: GenreRankingPage,
        history: HistoryRankingPage
      };
    }

    start() {
      var pageName = this.document.body.dataset.page;
      var PageClass = this.pages[pageName];
      if (!PageClass) return;
      new PageClass({
        app: this.app,
        document: this.document,
        window: this.window
      }).init();
    }
  }

  class HomeRankingPage {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.summaryRenderer = new GenerationSummaryRenderer(dependencies.app);
    }

    init() {
      var genreLinks = this.app.qs("[data-genre-links]");
      if (genreLinks) this.app.renderGenreLinks(genreLinks);

      this.loadOverallPreview();
      this.loadPreview("latest/trending.json", "trending", "[data-home-trending]");
      this.loadPreview("latest/discovery.json", "discovery", "[data-home-discovery]");
      this.loadGenerationSummary();
    }

    loadOverallPreview() {
      var app = this.app;
      app.showState("overall", "ランキングJSONを読み込んでいます。");
      app.loadJson("latest/overall.json")
        .then(function (document) {
          app.showState("overall", "");
          app.setText("[data-updated-at]", app.formatDateTime(document.generatedAt));
          app.renderRankingList(app.qs("[data-home-overall]"), (document.ranking || []).slice(0, 5), { compact: true });
        })
        .catch(function () {
          app.showState("overall", "まだランキングデータがありません。次回の実データ生成後に表示されます。");
          app.setText("[data-updated-at]", "未生成");
        });
    }

    loadPreview(path, stateName, listSelector) {
      var app = this.app;
      app.showState(stateName, "読み込み中です。");
      app.loadJson(path)
        .then(function (document) {
          app.showState(stateName, "");
          app.renderRankingList(app.qs(listSelector), (document.ranking || []).slice(0, 3), { compact: true });
        })
        .catch(function () {
          app.showState(stateName, "次回の実データ生成後に表示されます。");
        });
    }

    loadGenerationSummary() {
      var app = this.app;
      var renderer = this.summaryRenderer;
      app.showState("summary", "生成サマリーを読み込んでいます。");
      app.loadJson("latest/generation-summary.json")
        .then(function (summary) {
          app.showState("summary", "");
          renderer.render(summary);
        })
        .catch(function () {
          app.showState("summary", "生成サマリーはまだありません。");
          app.setText("[data-summary-status]", "未生成");
        });
    }
  }

  class GenerationSummaryRenderer {
    constructor(app) {
      this.app = app;
    }

    render(summary) {
      var app = this.app;
      var collection = summary.collection || {};
      var retention = summary.retention || {};
      var sourceResults = collection.sourceResults || [];
      var freshness = app.generationFreshness(summary.generatedAt);
      var skippedCount = sourceResults.filter(function (result) {
        return result.status === "skipped";
      }).length;
      var statusText = skippedCount > 0 && freshness.level === "ok" ? "一部スキップ" : freshness.text;
      var statusNode = app.qs("[data-summary-status]");

      app.setText("[data-summary-status]", statusText);
      if (statusNode) statusNode.className = "status-pill summary-status " + freshness.level;
      app.setText("[data-summary-input]", app.formatNumber(summary.inputVideos || 0) + "本");
      app.setText("[data-summary-public]", app.formatNumber(collection.publicVideos || 0) + "本");
      app.setText("[data-summary-ranking]", app.formatNumber(summary.rankingVideos || 0) + "本");
      app.setText("[data-summary-quota]", app.formatNumber(collection.estimatedQuotaUnits || 0) + " units");
      app.setText(
        "[data-summary-retention]",
        "保持期間チェック: 履歴削除 " + app.formatNumber(retention.historyDeleted || 0)
          + "件 / 詳細削除 " + app.formatNumber(retention.videoDetailsDeleted || 0) + "件"
      );
      app.setText("[data-summary-freshness]", freshness.detail);

      this.renderHealthChecks("[data-summary-health]", app.generationHealth(summary).slice(0, 3));
      this.renderSourceResults(sourceResults);
    }

    renderHealthChecks(selector, checks) {
      var app = this.app;
      var container = app.qs(selector);
      if (!container) return;
      app.clear(container);

      checks.forEach(function (check) {
        var item = app.el("article", { className: "health-item " + check.level });
        item.appendChild(app.el("strong", { text: check.title }));
        item.appendChild(app.el("span", { text: check.detail }));
        container.appendChild(item);
      });
    }

    renderSourceResults(sourceResults) {
      var app = this.app;
      var container = app.qs("[data-summary-sources]");
      if (!container) return;
      app.clear(container);

      if (!sourceResults.length) {
        container.appendChild(app.el("p", { className: "muted-text", text: "収集元記録なし" }));
        return;
      }

      sourceResults.slice(0, 8).forEach(function (result) {
        var row = app.el("article", { className: "source-row" });
        var main = app.el("div", { className: "source-main" });
        main.appendChild(app.el("strong", { text: result.source || "source" }));
        main.appendChild(app.el("span", {
          text: app.formatNumber(result.collected || 0) + " / " + app.formatNumber(result.requested || 0) + "件"
        }));
        row.appendChild(main);
        row.appendChild(app.el("span", {
          className: "source-status " + (result.status === "skipped" ? "skipped" : "ok"),
          text: result.status === "skipped" ? "Skipped" : "OK"
        }));
        container.appendChild(row);
      });
    }
  }

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
      this.searchParams = new URLSearchParams(this.window.location.search);
      this.initialView = this.searchParams.get("view") || "overall";
      this.initialQuery = this.searchParams.get("q") || "";
      this.initialDate = this.searchParams.get("date") || "";
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

    periodLabel(path) {
      if (path.indexOf("today.json") >= 0) return "本日";
      if (path.indexOf("seven-days.json") >= 0) return "7日間";
      if (path.indexOf("trending.json") >= 0) return "急上昇";
      if (path.indexOf("discovery.json") >= 0) return "発掘";
      if (this.currentView === "history") return "過去日";
      return "24時間";
    }

    titleForView(view) {
      if (view === "today") return "本日ランキング";
      if (view === "seven-days") return "7日間ランキング";
      if (view === "trending") return "急上昇ランキング";
      if (view === "discovery") return "発掘ランキング";
      if (view === "history") return "過去日ランキング";
      return "総合ランキング";
    }

    eyebrowForView(view) {
      if (view === "today") return "Today ranking";
      if (view === "seven-days") return "Seven day ranking";
      if (view === "trending") return "Trending ranking";
      if (view === "discovery") return "Discovery ranking";
      if (view === "history") return "History ranking";
      return "Overall ranking";
    }

    renderOverallEntries(entries, query) {
      var app = this.app;
      app.showState("overall", "");
      if (entries.length) {
        app.renderRankingList(this.container, entries);
        return;
      }

      if (query) {
        app.renderEmptyState(this.container, {
          title: "検索条件に一致する動画がありません。",
          message: this.periodLabel(this.currentPath) + "ランキング内で、別のキーワードを試してください。"
        });
        return;
      }

      if (this.currentPath.indexOf("today.json") >= 0) {
        app.renderEmptyState(this.container, {
          title: "本日分の増加データはまだありません。",
          message: "本日ランキングは、今日の複数回の収集結果から再生数の増加が確認できた動画だけを表示します。",
          actionText: "24時間ランキングを見る",
          href: "rankings/overall-ranking.html"
        });
        return;
      }

      app.renderEmptyState(this.container, {
        title: this.periodLabel(this.currentPath) + "ランキングはまだありません。",
        message: "次回のランキング生成後に表示されます。"
      });
    }

    setActiveButton(view) {
      this.viewButtons.forEach(function (item) {
        item.classList.toggle("active", item.getAttribute("data-ranking-view") === view);
      });
    }

    selectedHistoryItem() {
      var historySelect = this.historySelect;
      if (!historySelect) return null;
      return this.historyItems.find(function (item) {
        return item.path === historySelect.value;
      }) || null;
    }

    updateViewUrl(view, query) {
      var url = new URL(this.window.location.href);
      if (view === "overall") {
        url.searchParams.delete("view");
      } else {
        url.searchParams.set("view", view);
      }

      if (query) {
        url.searchParams.set("q", query);
      } else {
        url.searchParams.delete("q");
      }

      if (view === "history") {
        var item = this.selectedHistoryItem();
        if (item && item.date) url.searchParams.set("date", item.date);
      } else {
        url.searchParams.delete("date");
      }
      this.window.history.replaceState(null, "", url.toString());
    }

    toggleHistoryPicker(view) {
      if (!this.historyPicker) return;
      this.historyPicker.hidden = view !== "history";
    }

    applyViewState(view) {
      this.currentView = view || "overall";
      this.setActiveButton(this.currentView);
      this.toggleHistoryPicker(this.currentView);
      this.app.setText("[data-ranking-eyebrow]", this.eyebrowForView(this.currentView));
      this.app.setText("[data-ranking-title]", this.titleForView(this.currentView));
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

          if (self.historySelect && !self.historySelect.children.length) {
            self.historyItems.forEach(function (item) {
              self.historySelect.appendChild(self.app.el("option", {
                text: item.date + " / " + self.app.formatNumber(item.totalVideos) + "本",
                value: item.path
              }));
            });
          }

          var selectedItem = self.historyItems.find(function (item) {
            return item.date === self.initialDate;
          }) || self.historyItems[0];
          if (self.historySelect) self.historySelect.value = selectedItem.path;
          self.loadOverallHistory(selectedItem.path, shouldUpdateUrl);
        })
        .catch(function () {
          self.app.showState("overall", "履歴インデックスを読み込めませんでした。");
          self.app.clear(self.container);
        });
    }

    renderFilteredEntries() {
      var query = this.input ? this.input.value.trim().toLowerCase() : "";
      var filtered = this.allEntries.filter(function (entry) {
        var genreText = (entry.genres || []).map(function (genre) {
          return genre.name + " " + genre.slug;
        }).join(" ");
        return (entry.title + " " + entry.channelName + " " + genreText).toLowerCase().indexOf(query) >= 0;
      });
      this.renderOverallEntries(filtered, query);
    }
  }

  class GenreRankingPage {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.window = dependencies.window;
      this.select = this.app.qs("[data-genre-select]");
      this.selected = new URLSearchParams(this.window.location.search).get("genre") || "gaming";
    }

    init() {
      var self = this;
      this.app.GENRES.forEach(function (genre) {
        var option = self.app.el("option", { text: genre.name, value: genre.slug });
        if (genre.slug === self.selected) option.selected = true;
        self.select.appendChild(option);
      });

      this.select.addEventListener("change", function () {
        var url = new URL(self.window.location.href);
        url.searchParams.set("genre", self.select.value);
        self.window.location.href = url.toString();
      });

      this.loadGenre(this.selected);
    }

    loadGenre(slug) {
      var app = this.app;
      var container = app.qs("[data-genre-ranking]");
      app.showState("genre", "ジャンルランキングJSONを読み込んでいます。");
      app.loadJson("latest/genres/" + encodeURIComponent(slug) + ".json")
        .then(function (document) {
          app.showState("genre", "");
          app.setText("[data-genre-title]", (document.genre && document.genre.name ? document.genre.name : slug) + "ランキング");
          app.setText("[data-updated-at]", "最終更新 " + app.formatDateTime(document.generatedAt));
          app.setText("[data-genre-status]", document.status === "official" ? "正式ランキング" : "参考順位");
          app.renderRankingList(container, document.ranking || []);
        })
        .catch(function () {
          app.setText("[data-genre-title]", "ジャンル別ランキング");
          app.setText("[data-updated-at]", "未生成");
          app.setText("[data-genre-status]", "集計中");
          app.showState("genre", "このジャンルのランキングデータはまだありません。");
          app.clear(container);
        });
    }
  }

  class HistoryRankingPage {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.window = dependencies.window;
      this.select = this.app.qs("[data-history-select]");
    }

    init() {
      var self = this;
      this.app.showState("history", "履歴インデックスを読み込んでいます。");
      this.app.loadJson("latest/history-index.json")
        .then(function (index) {
          var items = (index.items || []).slice().reverse();
          self.app.showState("history", "");
          if (!items.length) {
            self.app.showState("history", "過去日ランキングはまだありません。");
            return;
          }

          items.forEach(function (item) {
            self.select.appendChild(self.app.el("option", {
              text: item.date + " / " + self.app.formatNumber(item.totalVideos) + "本",
              value: item.path
            }));
          });

          var selectedDate = new URLSearchParams(self.window.location.search).get("date");
          var selectedItem = items.find(function (item) {
            return item.date === selectedDate;
          }) || items[0];
          self.select.value = selectedItem.path;
          self.select.addEventListener("change", function () {
            self.loadHistory(self.select.value);
          });
          self.loadHistory(selectedItem.path);
        })
        .catch(function () {
          self.app.showState("history", "履歴インデックスを読み込めませんでした。");
        });
    }

    loadHistory(path) {
      var app = this.app;
      var container = app.qs("[data-history-ranking]");
      app.showState("history", "過去日ランキングJSONを読み込んでいます。");
      app.loadJson(path)
        .then(function (document) {
          app.showState("history", "");
          app.setText("[data-history-title]", "過去日ランキング");
          app.setText("[data-updated-at]", "生成 " + app.formatDateTime(document.generatedAt));
          app.renderRankingList(container, document.ranking || []);
        })
        .catch(function () {
          app.showState("history", "過去日ランキングを読み込めませんでした。");
          app.clear(container);
        });
    }
  }

  new RankingPageRouter({
    app: window.YTRank,
    document: document,
    window: window
  }).start();
})();
