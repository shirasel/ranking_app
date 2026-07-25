(function () {
  "use strict";

  class VideoDetailRenderer {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.container = dependencies.container;
      this.mediaRenderer = dependencies.mediaRenderer || new window.YTRankVideo.VideoMediaRenderer({ app: this.app });
      this.metricRenderer = dependencies.metricRenderer || new window.YTRankVideo.MetricRenderer({ app: this.app });
      this.scoreRenderer = dependencies.scoreRenderer || new window.YTRankVideo.ScoreBreakdownRenderer({ app: this.app });
      this.statisticsRenderer = dependencies.statisticsRenderer || new window.YTRankVideo.StatisticsHistoryRenderer({ app: this.app });
      this.rankingHistoryRenderer = dependencies.rankingHistoryRenderer || new window.YTRankVideo.RankingHistoryRenderer({ app: this.app });
    }

    render(document, history, rankHistory) {
      var app = this.app;
      var entry = document.video;
      app.clear(this.container);
      app.setText("[data-video-title]", entry.title);
      app.setText("[data-updated-at]", "最終更新 " + app.formatDateTime(document.generatedAt));

      var left = app.el("div");
      left.appendChild(this.mediaRenderer.create(entry));
      left.appendChild(this.createScorePanel(entry.scoreBreakdown || {}));
      left.appendChild(this.rankingHistoryRenderer.create(rankHistory));
      left.appendChild(this.statisticsRenderer.create(history));
      this.container.appendChild(left);
      this.container.appendChild(this.createSidePanel(entry));
    }

    createSidePanel(entry) {
      var side = this.app.el("aside", { className: "detail-panel" });
      side.appendChild(this.app.el("h2", { text: "ランキング指標" }));
      side.appendChild(this.metricRenderer.create(entry));
      return side;
    }

    createScorePanel(breakdown) {
      var panel = this.app.el("section", { className: "history-panel" });
      panel.appendChild(this.app.el("h2", { text: "スコア内訳" }));
      panel.appendChild(this.scoreRenderer.create(breakdown));
      return panel;
    }
  }

  window.YTRankVideo = window.YTRankVideo || {};
  window.YTRankVideo.VideoDetailRenderer = VideoDetailRenderer;
})();
