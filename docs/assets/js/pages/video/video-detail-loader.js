(function () {
  "use strict";

  class VideoDetailLoader {
    constructor(dependencies) {
      this.app = dependencies.app;
    }

    load(videoId) {
      var encodedId = encodeURIComponent(videoId);
      return Promise.all([
        this.app.loadJson("videos/" + encodedId + ".json"),
        this.app.loadJson("statistics/videos/" + encodedId + ".json").catch(function () { return null; }),
        this.app.loadJson("rankings/videos/" + encodedId + ".json").catch(function () { return null; })
      ]).then(function (results) {
        return {
          videoDocument: results[0],
          statisticsHistory: results[1],
          rankingHistory: results[2]
        };
      });
    }
  }

  window.YTRankVideo = window.YTRankVideo || {};
  window.YTRankVideo.VideoDetailLoader = VideoDetailLoader;
})();
