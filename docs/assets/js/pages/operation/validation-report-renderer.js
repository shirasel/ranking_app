(function () {
  "use strict";

  class ValidationReportRenderer {
    constructor(dependencies) {
      this.app = dependencies.app;
    }

    render(report) {
      var app = this.app;
      var statusNode = app.qs("[data-validation-status]");
      var messages = [].concat(report.errors || []).concat(report.warnings || []);
      var level = report.status === "passed" ? "ok" : "stale";

      app.setText("[data-validation-status]", report.status === "passed" ? "Passed" : "Failed");
      if (statusNode) statusNode.className = "status-pill " + level;
      app.setText("[data-validation-updated-at]", "検証対象 " + app.formatDateTime(report.generatedAt));
      this.renderMessages(messages);
    }

    renderMessages(messages) {
      var app = this.app;
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
  }

  window.YTRankOperation = window.YTRankOperation || {};
  window.YTRankOperation.ValidationReportRenderer = ValidationReportRenderer;
})();
