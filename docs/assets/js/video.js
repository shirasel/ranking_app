(function () {
  "use strict";

  class VideoDetailPage {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.window = dependencies.window;
      this.container = this.app.qs("[data-video-detail]");
      this.videoId = new URLSearchParams(this.window.location.search).get("id");
      this.videoIdValidator = dependencies.videoIdValidator || new window.YTRankVideo.VideoIdValidator();
      this.loader = dependencies.loader || new window.YTRankVideo.VideoDetailLoader({ app: this.app });
      this.renderer = dependencies.renderer || new window.YTRankVideo.VideoDetailRenderer({
        app: this.app,
        container: this.container
      });
    }

    init() {
      if (!this.videoId) {
        this.app.showState("video", "動画IDが指定されていません。");
        return;
      }
      if (!this.videoIdValidator.isValid(this.videoId)) {
        this.app.showState("video", "動画IDの形式が正しくありません。");
        return;
      }
      this.loadVideo(this.videoId);
    }

    loadVideo(videoId) {
      var app = this.app;
      var renderer = this.renderer;
      app.showState("video", "動画詳細JSONを読み込んでいます。");
      this.loader.load(videoId)
        .then(function (results) {
          app.showState("video", "");
          renderer.render(results.videoDocument, results.statisticsHistory, results.rankingHistory);
        })
        .catch(function () {
          app.showState("video", "動画詳細データを読み込めませんでした。");
        });
    }
  }

  new VideoDetailPage({
    app: window.YTRank,
    window: window
  }).init();
})();
