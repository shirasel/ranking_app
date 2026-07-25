(function () {
  "use strict";

  class OperationLogPage {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.summaryLoader = dependencies.summaryLoader || new window.YTRankOperation.OperationJsonLoader({
        app: dependencies.app,
        path: "latest/generation-summary.json"
      });
      this.validationLoader = dependencies.validationLoader || new window.YTRankOperation.OperationJsonLoader({
        app: dependencies.app,
        path: "latest/validation-report.json"
      });
      this.summaryRenderer = dependencies.summaryRenderer || new window.YTRankOperation.OperationSummaryRenderer({
        app: dependencies.app,
        healthRenderer: new window.YTRankClasses.HealthCheckRenderer({ app: dependencies.app }),
        sourceRenderer: new window.YTRankClasses.SourceResultsRenderer({ app: dependencies.app })
      });
      this.validationRenderer = dependencies.validationRenderer || new window.YTRankOperation.ValidationReportRenderer({
        app: dependencies.app
      });
    }

    init() {
      this.loadOperationSummary();
      this.loadValidationReport();
    }

    loadOperationSummary() {
      var app = this.app;
      var renderer = this.summaryRenderer;
      app.showState("operation", "生成サマリーを読み込んでいます。");
      this.summaryLoader.load()
        .then(function (summary) {
          app.showState("operation", "");
          renderer.render(summary);
        })
        .catch(function () {
          app.showState("operation", "生成サマリーを読み込めませんでした。");
          app.setText("[data-operation-updated-at]", "未生成");
        });
    }

    loadValidationReport() {
      var app = this.app;
      var renderer = this.validationRenderer;
      this.validationLoader.load()
        .then(function (report) {
          renderer.render(report);
        })
        .catch(function () {
          app.setText("[data-validation-status]", "未生成");
          app.setText("[data-validation-updated-at]", "検証レポートはまだありません。");
        });
    }
  }

  window.YTRankOperation = window.YTRankOperation || {};
  window.YTRankOperation.OperationLogPage = OperationLogPage;
})();
