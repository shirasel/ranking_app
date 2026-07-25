(function () {
  "use strict";

  class VideoMediaRenderer {
    constructor(dependencies) {
      this.app = dependencies.app;
    }

    create(entry) {
      var app = this.app;
      var media = app.el("section", { className: "detail-media" });
      var thumb = app.el("div", { className: "thumbnail" });
      if (entry.thumbnailUrl) {
        thumb.appendChild(app.el("img", { src: entry.thumbnailUrl, alt: entry.title + " のサムネイル" }));
      }
      media.appendChild(thumb);
      media.appendChild(app.el("h2", { text: entry.title }));

      var meta = app.el("div", { className: "video-meta" });
      meta.appendChild(app.el("span", { text: entry.channelName }));
      meta.appendChild(app.el("span", { text: app.formatDateTime(entry.publishedAt) }));
      media.appendChild(meta);

      var tags = app.el("div", { className: "genre-tags" });
      app.appendGenreTags(tags, entry.genres);
      media.appendChild(tags);

      var youtubeUrl = app.safeYouTubeUrl(entry.videoId);
      if (youtubeUrl) {
        media.appendChild(app.el("a", { className: "button primary", text: "YouTubeで見る", href: youtubeUrl }));
      }
      return media;
    }
  }

  window.YTRankVideo = window.YTRankVideo || {};
  window.YTRankVideo.VideoMediaRenderer = VideoMediaRenderer;
})();
