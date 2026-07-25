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

      if (settings.summary) {
        container.appendChild(createSummary(app, sourceResults));
      }

      if (settings.grouped) {
        createGroups(app, sourceResults, settings).forEach(function (group) {
          container.appendChild(group);
        });
        return;
      }

      sourceResults.slice(0, settings.limit || sourceResults.length).forEach(function (source) {
        container.appendChild(createRow(app, source, settings));
      });
    }
  }

  function createSummary(app, sourceResults) {
    var summary = app.el("div", { className: "source-summary-grid" });
    sourceGroups(sourceResults).forEach(function (group) {
      var item = app.el("article", { className: "source-summary-card" });
      item.appendChild(app.el("span", { text: group.label }));
      item.appendChild(app.el("strong", { text: app.formatNumber(group.collected) + "件" }));
      item.appendChild(app.el("small", {
        text: app.formatNumber(group.sources) + "元 / 要求 " + app.formatNumber(group.requested) + "件"
      }));
      summary.appendChild(item);
    });
    return summary;
  }

  function createGroups(app, sourceResults, settings) {
    return sourceGroups(sourceResults).map(function (group) {
      var section = app.el("section", { className: "source-group" });
      var heading = app.el("div", { className: "source-group-heading" });
      heading.appendChild(app.el("strong", { text: group.label }));
      heading.appendChild(app.el("span", {
        text: app.formatNumber(group.collected) + " / " + app.formatNumber(group.requested) + "件"
      }));
      section.appendChild(heading);
      group.items.slice(0, settings.groupLimit || group.items.length).forEach(function (source) {
        section.appendChild(createRow(app, source, settings));
      });
      return section;
    });
  }

  function sourceGroups(sourceResults) {
    var groups = {};
    sourceResults.forEach(function (source) {
      var type = sourceType(source.source || "");
      groups[type.key] = groups[type.key] || {
        key: type.key,
        label: type.label,
        order: type.order,
        requested: 0,
        collected: 0,
        sources: 0,
        items: []
      };
      groups[type.key].requested += Number(source.requested || 0);
      groups[type.key].collected += Number(source.collected || 0);
      groups[type.key].sources += 1;
      groups[type.key].items.push(source);
    });
    return Object.keys(groups).map(function (key) { return groups[key]; })
      .sort(function (a, b) { return a.order - b.order; });
  }

  function sourceType(sourceName) {
    if (sourceName.indexOf("category-popular:") === 0) {
      return { key: "category", label: "カテゴリ人気", order: 1 };
    }
    if (sourceName.indexOf("keyword:") === 0) {
      return { key: "keyword", label: "固定キーワード", order: 2 };
    }
    if (sourceName.indexOf("recent-view-count:") === 0) {
      return { key: "recent", label: "新着再生数順", order: 3 };
    }
    if (sourceName.indexOf("tracked-previous") === 0) {
      return { key: "tracked", label: "追跡動画", order: 4 };
    }
    if (sourceName.indexOf("popular:") === 0) {
      return { key: "popular", label: "全体人気", order: 5 };
    }
    if (sourceName.indexOf("channel:") === 0) {
      return { key: "channel", label: "チャンネル", order: 6 };
    }
    if (sourceName.indexOf("manual-videos") === 0) {
      return { key: "manual", label: "手動指定", order: 7 };
    }
    return { key: "other", label: "その他", order: 99 };
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
