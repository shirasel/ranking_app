(function () {
  "use strict";

  class HomeRankingPage {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.summaryRenderer = new window.YTRankPages.GenerationSummaryRenderer({
        app: dependencies.app,
        healthRenderer: new window.YTRankClasses.HealthCheckRenderer({ app: dependencies.app }),
        sourceRenderer: new window.YTRankClasses.SourceResultsRenderer({ app: dependencies.app })
      });
    }

    init() {
      this.loadGenreLinks();
      this.loadOverallPreview();
      this.loadPreview("latest/trending.json", "trending", "[data-home-trending]");
      this.loadPreview("latest/discovery.json", "discovery", "[data-home-discovery]");
      this.loadGenerationSummary();
    }

    loadGenreLinks() {
      var app = this.app;
      var genreLinks = app.qs("[data-genre-links]");
      if (!genreLinks) return;
      app.loadGenreCatalog()
        .then(function () {
          app.renderGenreLinks(genreLinks);
        })
        .catch(function () {
          app.renderGenreLinks(genreLinks);
        });
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

  window.YTRankPages = window.YTRankPages || {};
  window.YTRankPages.HomeRankingPage = HomeRankingPage;
})();
