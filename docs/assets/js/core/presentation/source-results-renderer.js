(function () {
  "use strict";

  class SourceResultsRenderer {
    constructor(dependencies) {
      this.app = dependencies.app;
    }

    render(selector, sourceResults, options) {
      var app = this.app;
      var container = app.qs(selector);
      var settings = options || {};
      if (!container) return;
      app.clear(container);

      if (!sourceResults.length) {
        container.appendChild(app.el("p", { className: "muted-text", text: "収集元記録なし" }));
        return;
      }

      sourceResults.slice(0, settings.limit || sourceResults.length).forEach(function (source) {
        container.appendChild(createRow(app, source, settings));
      });
    }
  }

  function createRow(app, source, settings) {
    var row = app.el("article", {
      className: "source-row" + (settings.rowClass ? " " + settings.rowClass : "")
    });
    var main = app.el("div", { className: "source-main" });
    main.appendChild(app.el("strong", { text: source.source || "source" }));
    main.appendChild(app.el("span", {
      text: settings.detailed
        ? "要求 " + app.formatNumber(source.requested || 0) + "件 / 収集 " + app.formatNumber(source.collected || 0) + "件"
        : app.formatNumber(source.collected || 0) + " / " + app.formatNumber(source.requested || 0) + "件"
    }));
    if (settings.showMessage && source.message) main.appendChild(app.el("span", { text: source.message }));
    row.appendChild(main);
    row.appendChild(app.el("span", {
      className: "source-status " + (source.status === "skipped" ? "skipped" : "ok"),
      text: source.status === "skipped" ? "Skipped" : "OK"
    }));
    return row;
  }

  window.YTRankClasses = window.YTRankClasses || {};
  window.YTRankClasses.SourceResultsRenderer = SourceResultsRenderer;
})();
