(function () {
  "use strict";

  class OperationSummaryRenderer {
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
      var statusNode = app.qs("[data-operation-status]");
      var skippedCount = sourceResults.filter(function (source) {
        return source.status === "skipped";
      }).length;

      app.setText("[data-operation-updated-at]", "最終生成 " + app.formatDateTime(summary.generatedAt));
      app.setText("[data-operation-freshness]", freshness.detail);
      app.setText("[data-operation-input]", app.formatNumber(summary.inputVideos || 0) + "本");
      app.setText("[data-operation-candidates]", app.formatNumber(collection.uniqueCandidateIds || 0) + "件");
      app.setText("[data-operation-fetched]", app.formatNumber(collection.fetchedVideoIds || 0) + "件");
      app.setText("[data-operation-quota]", app.formatNumber(collection.estimatedQuotaUnits || 0) + " units");
      app.setText("[data-operation-status]", skippedCount > 0 && freshness.level === "ok" ? "一部スキップ" : freshness.text);
      if (statusNode) statusNode.className = "status-pill " + freshness.level;
      app.setText("[data-operation-ranking]", app.formatNumber(summary.rankingVideos || 0) + "本");
      app.setText("[data-operation-public]", app.formatNumber(collection.publicVideos || 0) + "本");
      app.setText("[data-operation-genres]", app.formatNumber(summary.genreRankings || 0));
      app.setText("[data-operation-history-deleted]", app.formatNumber(retention.historyDeleted || 0) + "件");
      app.setText("[data-operation-details-deleted]", app.formatNumber(retention.videoDetailsDeleted || 0) + "件");

      this.sourceRenderer.render("[data-operation-sources]", sourceResults, {
        detailed: true,
        rowClass: "operation-source-row",
        showMessage: true
      });
      this.healthRenderer.render("[data-operation-health]", app.generationHealth(summary));
    }
  }

  window.YTRankOperation = window.YTRankOperation || {};
  window.YTRankOperation.OperationSummaryRenderer = OperationSummaryRenderer;
})();
