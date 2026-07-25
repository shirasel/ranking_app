(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class UrlService {
    constructor(dependencies) {
      this.document = dependencies.document;
    }

    pageDepthPrefix() {
      return this.document.body.dataset.page === "home" ? "" : "../";
    }

    dataUrl(path) {
      return this.pageDepthPrefix() + "data/" + path.replace(/^\/+/, "");
    }

    pageUrl(path) {
      return this.pageDepthPrefix() + path.replace(/^\/+/, "");
    }
  }

  window.YTRankClasses.UrlService = UrlService;
})();
