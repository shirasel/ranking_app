(function () {
  "use strict";

  class RankingHistoryRenderer {
    constructor(dependencies) {
      this.app = dependencies.app;
    }

    create(history) {
      var rankings = history && Array.isArray(history.rankings) ? history.rankings : [];
      if (!rankings.length) {
        return this.wrap(this.app.el("p", { className: "muted-text", text: "順位履歴は次回以降の生成で蓄積されます。" }));
      }

      var wrapper = this.app.el("div", { className: "trend-table-wrap" });
      wrapper.appendChild(this.createChart(rankings));
      wrapper.appendChild(this.createTable(rankings));
      return this.wrap(wrapper);
    }

    wrap(content) {
      var panel = this.app.el("section", { className: "history-panel" });
      panel.appendChild(this.app.el("h2", { text: "順位推移" }));
      panel.appendChild(content);
      return panel;
    }

    createChart(rankings) {
      var app = this.app;
      var chart = app.el("div", { className: "rank-timeline", title: "総合順位推移" });
      rankings.slice(-14).forEach(function (row) {
        var point = app.el("div", { className: "rank-point" });
        point.style.height = Math.max(10, 96 - Math.min(90, Number(row.rank || 100))) + "px";
        point.setAttribute("aria-label", app.formatDateTime(row.capturedAt) + " " + row.rank + "位");
        point.appendChild(app.el("span", { text: row.rank }));
        chart.appendChild(point);
      });
      return chart;
    }

    createTable(rankings) {
      var app = this.app;
      var table = app.el("table", { className: "trend-table" });
      var thead = app.el("thead");
      var headerRow = app.el("tr");
      ["取得日時", "順位", "前回順位", "変動", "スコア"].forEach(function (label) {
        headerRow.appendChild(app.el("th", { text: label }));
      });
      thead.appendChild(headerRow);
      table.appendChild(thead);

      var tbody = app.el("tbody");
      rankings.slice(-10).reverse().forEach(function (row) {
        var tr = app.el("tr");
        tr.appendChild(app.el("td", { text: app.formatDateTime(row.capturedAt) }));
        tr.appendChild(app.el("td", { text: row.rank + "位" }));
        tr.appendChild(app.el("td", { text: row.previousRank ? row.previousRank + "位" : "NEW" }));
        tr.appendChild(app.el("td", { text: row.rankChange === null || row.rankChange === undefined ? "-" : String(row.rankChange) }));
        tr.appendChild(app.el("td", { text: app.formatScore(row.normalizedScore) }));
        tbody.appendChild(tr);
      });
      table.appendChild(tbody);
      return table;
    }
  }

  window.YTRankVideo = window.YTRankVideo || {};
  window.YTRankVideo.RankingHistoryRenderer = RankingHistoryRenderer;
})();
