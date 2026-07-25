(function () {
  "use strict";

  class StatisticsHistoryRenderer {
    constructor(dependencies) {
      this.app = dependencies.app;
    }

    create(history) {
      var statistics = history && Array.isArray(history.statistics) ? history.statistics : [];
      if (!statistics.length) {
        return this.wrap("統計推移", this.app.el("p", { className: "muted-text", text: "統計履歴は次回以降の生成で蓄積されます。" }));
      }

      var wrapper = this.app.el("div", { className: "trend-table-wrap" });
      wrapper.appendChild(this.createTable(statistics));
      wrapper.appendChild(this.createViewTrend(statistics));
      return this.wrap("統計推移", wrapper);
    }

    wrap(title, content) {
      var panel = this.app.el("section", { className: "history-panel" });
      panel.appendChild(this.app.el("h2", { text: title }));
      panel.appendChild(content);
      return panel;
    }

    createTable(statistics) {
      var app = this.app;
      var table = app.el("table", { className: "trend-table" });
      var thead = app.el("thead");
      var headerRow = app.el("tr");
      ["取得日時", "再生回数", "高評価", "コメント", "登録者"].forEach(function (label) {
        headerRow.appendChild(app.el("th", { text: label }));
      });
      thead.appendChild(headerRow);
      table.appendChild(thead);

      var tbody = app.el("tbody");
      statistics.slice(-10).reverse().forEach(function (row) {
        var tr = app.el("tr");
        tr.appendChild(app.el("td", { text: app.formatDateTime(row.capturedAt) }));
        tr.appendChild(app.el("td", { text: app.formatNumber(row.viewCount) }));
        tr.appendChild(app.el("td", { text: app.formatNumber(row.likeCount) }));
        tr.appendChild(app.el("td", { text: app.formatNumber(row.commentCount) }));
        tr.appendChild(app.el("td", { text: app.formatNumber(row.subscriberCount) }));
        tbody.appendChild(tr);
      });
      table.appendChild(tbody);
      return table;
    }

    createViewTrend(statistics) {
      var app = this.app;
      var values = statistics.map(function (row) { return Number(row.viewCount || 0); });
      var max = Math.max.apply(null, values.concat([1]));
      var chart = app.el("div", { className: "trend-bars", title: "再生回数推移" });
      statistics.slice(-14).forEach(function (row) {
        var value = Number(row.viewCount || 0);
        var bar = app.el("div", { className: "trend-bar" });
        bar.style.height = Math.max(8, Math.round((value / max) * 88)) + "px";
        bar.setAttribute("aria-label", app.formatDateTime(row.capturedAt) + " 再生 " + app.formatNumber(value));
        chart.appendChild(bar);
      });
      return chart;
    }
  }

  window.YTRankVideo = window.YTRankVideo || {};
  window.YTRankVideo.StatisticsHistoryRenderer = StatisticsHistoryRenderer;
})();
