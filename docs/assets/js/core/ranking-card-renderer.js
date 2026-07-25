(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class RankingCardRenderer {
    constructor(dependencies) {
      this.dom = dependencies.dom;
      this.formatter = dependencies.formatter;
      this.urlService = dependencies.urlService;
      this.genreRenderer = dependencies.genreRenderer;
      this.rankChangePresenter = dependencies.rankChangePresenter;
      this.youtubeUrlService = dependencies.youtubeUrlService;
    }

    create(entry, options) {
      options = options || {};
      var dom = this.dom;
      var card = dom.el("article", { className: "ranking-card" });
      card.appendChild(dom.el("div", { className: "rank-number", text: entry.rank }));

      var thumb = dom.el("a", {
        className: "thumbnail",
        href: this.urlService.pageUrl("videos/video-detail.html?id=" + encodeURIComponent(entry.videoId))
      });
      if (entry.thumbnailUrl) {
        thumb.appendChild(dom.el("img", { src: entry.thumbnailUrl, alt: entry.title + " のサムネイル" }));
      }
      card.appendChild(thumb);

      var main = dom.el("div", { className: "video-main" });
      var title = dom.el("h3", { className: "video-title" });
      title.appendChild(dom.el("a", {
        text: entry.title,
        href: this.urlService.pageUrl("videos/video-detail.html?id=" + encodeURIComponent(entry.videoId))
      }));
      main.appendChild(title);

      var meta = dom.el("div", { className: "video-meta" });
      meta.appendChild(dom.el("span", { text: entry.channelName }));
      meta.appendChild(dom.el("span", { text: "再生 " + this.formatter.formatNumber(entry.viewCount) }));
      meta.appendChild(dom.el("span", { text: "増加 " + this.formatter.formatNumber(entry.viewIncrease) }));
      meta.appendChild(dom.el("span", { text: this.formatter.formatDateTime(entry.publishedAt) }));
      main.appendChild(meta);

      var tags = dom.el("div", { className: "genre-tags" });
      this.genreRenderer.appendTags(tags, entry.genres);
      main.appendChild(tags);
      card.appendChild(main);

      card.appendChild(this.createExtra(entry, options));
      return card;
    }

    createExtra(entry, options) {
      var dom = this.dom;
      var extra = dom.el("div", { className: "ranking-extra" });
      extra.appendChild(dom.el("div", { className: "score", text: this.formatter.formatScore(entry.normalizedScore) }));
      var change = this.rankChangePresenter.label(entry);
      extra.appendChild(dom.el("span", { className: change.className, text: change.text }));

      if (!options.compact) {
        var scoreGrid = dom.el("div", { className: "score-grid" });
        scoreGrid.appendChild(dom.el("span", { text: "勢い " + this.formatter.formatScore(entry.scoreBreakdown && entry.scoreBreakdown.velocity) }));
        scoreGrid.appendChild(dom.el("span", { text: "反応 " + this.formatter.formatScore(entry.scoreBreakdown && entry.scoreBreakdown.engagement) }));
        scoreGrid.appendChild(dom.el("span", { text: "登録者比 " + this.formatter.formatScore(entry.scoreBreakdown && entry.scoreBreakdown.subscriberRatio) }));
        scoreGrid.appendChild(dom.el("span", { text: "鮮度 " + this.formatter.formatScore(entry.scoreBreakdown && entry.scoreBreakdown.freshness) }));
        extra.appendChild(scoreGrid);
      }

      var youtubeUrl = this.youtubeUrlService.safeUrl(entry.videoId);
      if (youtubeUrl && !options.compact) {
        extra.appendChild(dom.el("a", { className: "text-link", text: "YouTube", href: youtubeUrl }));
      }
      return extra;
    }
  }

  window.YTRankClasses.RankingCardRenderer = RankingCardRenderer;
})();
