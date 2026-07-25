(function () {
  "use strict";

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

  window.YTRankPages = window.YTRankPages || {};
  window.YTRankPages.HistoryRankingPage = HistoryRankingPage;
})();
