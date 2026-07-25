(function () {
  "use strict";

  class MetricRenderer {
    constructor(dependencies) {
      this.app = dependencies.app;
    }

    create(entry) {
      var app = this.app;
      var dl = app.el("dl");
      this.add(dl, "総合順位", entry.rank + "位");
      this.add(dl, "前回順位", entry.previousRank ? entry.previousRank + "位" : "NEW");
      this.add(dl, "順位変動", app.rankChangeLabel(entry).text);
      this.add(dl, "総合スコア", app.formatScore(entry.normalizedScore));
      this.add(dl, "Raw score", app.formatScore(entry.rawScore));
      this.add(dl, "再生回数", app.formatNumber(entry.viewCount));
      this.add(dl, "再生増加", app.formatNumber(entry.viewIncrease));
      this.add(dl, "7日間再生増加", entry.sevenDayViewIncrease === null || entry.sevenDayViewIncrease === undefined ? "-" : app.formatNumber(entry.sevenDayViewIncrease));
      this.add(dl, "高評価", app.formatNumber(entry.likeCount));
      this.add(dl, "コメント", app.formatNumber(entry.commentCount));
      this.add(dl, "登録者", app.formatNumber(entry.subscriberCount));
      return dl;
    }

    add(dl, label, value) {
      dl.appendChild(this.app.el("dt", { text: label }));
      dl.appendChild(this.app.el("dd", { text: value }));
    }
  }

  window.YTRankVideo = window.YTRankVideo || {};
  window.YTRankVideo.MetricRenderer = MetricRenderer;
})();
