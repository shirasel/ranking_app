(function () {
  "use strict";

  class GenerationSummaryRenderer {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.healthRenderer = dependencies.healthRenderer;
      this.sourceRenderer = dependencies.sourceRenderer;
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

      this.healthRenderer.render("[data-summary-health]", app.generationHealth(summary), 3);
      this.sourceRenderer.render("[data-summary-sources]", sourceResults, { summary: true, limit: 8 });
    }
  }

  window.YTRankPages = window.YTRankPages || {};
  window.YTRankPages.GenerationSummaryRenderer = GenerationSummaryRenderer;
})();
