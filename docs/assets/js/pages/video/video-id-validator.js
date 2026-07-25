(function () {
  "use strict";

  class VideoIdValidator {
    isValid(videoId) {
      return /^[A-Za-z0-9_-]{6,64}$/.test(videoId);
    }
  }

  window.YTRankVideo = window.YTRankVideo || {};
  window.YTRankVideo.VideoIdValidator = VideoIdValidator;
})();
