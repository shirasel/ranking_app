(function () {
  "use strict";

  class HealthCheckRenderer {
    constructor(dependencies) {
      this.app = dependencies.app;
    }

    render(selector, checks, limit) {
      var app = this.app;
      var container = app.qs(selector);
      if (!container) return;
      app.clear(container);

      var visibleChecks = typeof limit === "number" ? checks.slice(0, limit) : checks;
      visibleChecks.forEach(function (check) {
        var item = app.el("article", { className: "health-item " + check.level });
        item.appendChild(app.el("strong", { text: check.title }));
        item.appendChild(app.el("span", { text: check.detail }));
        container.appendChild(item);
      });
    }
  }

  window.YTRankClasses = window.YTRankClasses || {};
  window.YTRankClasses.HealthCheckRenderer = HealthCheckRenderer;
})();
