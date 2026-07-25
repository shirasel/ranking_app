(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class YTRankApplication {
    constructor(dependencies) {
      this.dom = dependencies.dom;
      this.urlService = dependencies.urlService;
      this.formatter = dependencies.formatter;
      this.repository = dependencies.repository;
      this.healthService = dependencies.healthService;
      this.genreCatalog = dependencies.genreCatalog;
      this.genreRenderer = dependencies.genreRenderer;
      this.listRenderer = dependencies.listRenderer;
      this.rankChangePresenter = dependencies.rankChangePresenter;
      this.youtubeUrlService = dependencies.youtubeUrlService;
    }

    get GENRES() { return this.genreCatalog.items; }
    qs(selector, root) { return this.dom.qs(selector, root); }
    qsa(selector, root) { return this.dom.qsa(selector, root); }
    el(tag, options) { return this.dom.el(tag, options); }
    clear(node) { return this.dom.clear(node); }
    setText(selector, value) { return this.dom.setText(selector, value); }
    dataUrl(path) { return this.urlService.dataUrl(path); }
    pageUrl(path) { return this.urlService.pageUrl(path); }
    loadJson(path) { return this.repository.load(path); }
    loadGenreCatalog() {
      var catalog = this.genreCatalog;
      return this.repository.load("latest/genre-catalog.json").then(function (document) {
        catalog.replace(document.genres || []);
        return catalog.items;
      });
    }
    formatDateTime(value) { return this.formatter.formatDateTime(value); }
    formatNumber(value) { return this.formatter.formatNumber(value); }
    formatScore(value) { return this.formatter.formatScore(value); }
    generationFreshness(value) { return this.healthService.freshness(value); }
    generationHealth(summary) { return this.healthService.health(summary); }
    rankChangeLabel(entry) { return this.rankChangePresenter.label(entry); }
    safeYouTubeUrl(videoId) { return this.youtubeUrlService.safeUrl(videoId); }
    appendGenreTags(container, genres) { return this.genreRenderer.appendTags(container, genres); }
    renderGenreLinks(container) { return this.genreRenderer.renderLinks(container); }
    renderRankingList(container, entries, options) { return this.listRenderer.render(container, entries, options); }
    renderEmptyState(container, options) { return this.listRenderer.renderEmpty(container, options); }

    showState(name, message) {
      var node = this.qs('[data-state="' + name + '"]');
      if (!node) return;
      this.clear(node);
      if (!message) return;
      node.appendChild(this.el("div", { className: "state", text: message }));
    }
  }

  window.YTRankClasses.YTRankApplication = YTRankApplication;
})();
