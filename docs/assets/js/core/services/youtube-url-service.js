(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class YouTubeUrlService {
    safeUrl(videoId) {
      if (!/^[A-Za-z0-9_-]{6,20}$/.test(videoId || "")) return null;
      return "https://www.youtube.com/watch?v=" + encodeURIComponent(videoId);
    }
  }

  window.YTRankClasses.YouTubeUrlService = YouTubeUrlService;
})();
