(function () {
  "use strict";

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

  window.YTRankPages = window.YTRankPages || {};
  window.YTRankPages.GenreRankingPage = GenreRankingPage;
})();
