(function () {
  "use strict";

  var app = window.YTRank;

  app.showState("operation", "生成サマリーを読み込んでいます。");
  app.loadJson("latest/generation-summary.json")
    .then(function (summary) {
      app.showState("operation", "");
      renderOperationSummary(summary);
    })
    .catch(function () {
      app.showState("operation", "生成サマリーを読み込めませんでした。");
      app.setText("[data-operation-updated-at]", "未生成");
    });
  loadValidationReport();

  function renderOperationSummary(summary) {
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

    renderSources(sourceResults);
    renderHealthChecks(app.generationHealth(summary));
  }

  function renderHealthChecks(checks) {
    var container = app.qs("[data-operation-health]");
    if (!container) return;
    app.clear(container);

    checks.forEach(function (check) {
      var item = app.el("article", { className: "health-item " + check.level });
      item.appendChild(app.el("strong", { text: check.title }));
      item.appendChild(app.el("span", { text: check.detail }));
      container.appendChild(item);
    });
  }

  function renderSources(sourceResults) {
    var container = app.qs("[data-operation-sources]");
    if (!container) return;
    app.clear(container);

    if (!sourceResults.length) {
      container.appendChild(app.el("p", { className: "muted-text", text: "収集元記録なし" }));
      return;
    }

    sourceResults.forEach(function (source) {
      var row = app.el("article", { className: "source-row operation-source-row" });
      var main = app.el("div", { className: "source-main" });
      main.appendChild(app.el("strong", { text: source.source || "source" }));
      main.appendChild(app.el("span", {
        text: "要求 " + app.formatNumber(source.requested || 0)
          + "件 / 収集 " + app.formatNumber(source.collected || 0) + "件"
      }));
      if (source.message) {
        main.appendChild(app.el("span", { text: source.message }));
      }
      row.appendChild(main);
      row.appendChild(app.el("span", {
        className: "source-status " + (source.status === "skipped" ? "skipped" : "ok"),
        text: source.status === "skipped" ? "Skipped" : "OK"
      }));
      container.appendChild(row);
    });
  }

  function loadValidationReport() {
    app.loadJson("latest/validation-report.json")
      .then(function (report) {
        renderValidationReport(report);
      })
      .catch(function () {
        app.setText("[data-validation-status]", "未生成");
        app.setText("[data-validation-updated-at]", "検証レポートはまだありません。");
      });
  }

  function renderValidationReport(report) {
    var statusNode = app.qs("[data-validation-status]");
    var messages = []
      .concat(report.errors || [])
      .concat(report.warnings || []);
    var level = report.status === "passed" ? "ok" : "stale";

    app.setText("[data-validation-status]", report.status === "passed" ? "Passed" : "Failed");
    if (statusNode) statusNode.className = "status-pill " + level;
    app.setText("[data-validation-updated-at]", "検証対象 " + app.formatDateTime(report.generatedAt));
    renderValidationMessages(messages);
  }

  function renderValidationMessages(messages) {
    var container = app.qs("[data-validation-messages]");
    if (!container) return;
    app.clear(container);

    if (!messages.length) {
      container.appendChild(app.el("p", { className: "muted-text", text: "検証エラーと警告はありません。" }));
      return;
    }

    messages.slice(0, 10).forEach(function (message) {
      container.appendChild(app.el("article", { className: "validation-message", text: message }));
    });
  }
})();
