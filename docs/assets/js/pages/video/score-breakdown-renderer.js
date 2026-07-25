(function () {
  "use strict";

  class ScoreBreakdownRenderer {
    constructor(dependencies) {
      this.app = dependencies.app;
    }

    create(breakdown) {
      var app = this.app;
      var list = app.el("div", { className: "bar-list" });
      [
        ["勢い", breakdown.velocity],
        ["7日間", breakdown.sevenDayVelocity],
        ["反応率", breakdown.engagement],
        ["登録者比", breakdown.subscriberRatio],
        ["鮮度", breakdown.freshness]
      ].forEach(function (item) {
        var value = Number(item[1] || 0);
        var row = app.el("div", { className: "bar-row" });
        var label = app.el("div", { className: "bar-label" });
        label.appendChild(app.el("span", { text: item[0] }));
        label.appendChild(app.el("span", { text: app.formatScore(value) }));
        var track = app.el("div", { className: "bar-track" });
        var fill = app.el("div", { className: "bar-fill" });
        fill.style.width = Math.max(0, Math.min(100, value)) + "%";
        track.appendChild(fill);
        row.appendChild(label);
        row.appendChild(track);
        list.appendChild(row);
      });
      return list;
    }
  }

  window.YTRankVideo = window.YTRankVideo || {};
  window.YTRankVideo.ScoreBreakdownRenderer = ScoreBreakdownRenderer;
})();
