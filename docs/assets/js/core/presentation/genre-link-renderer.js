(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class GenreLinkRenderer {
    constructor(dependencies) {
      this.dom = dependencies.dom;
      this.urlService = dependencies.urlService;
      this.genreCatalog = dependencies.genreCatalog;
    }

    appendTags(container, genres) {
      var self = this;
      (genres || []).forEach(function (genre) {
        container.appendChild(self.dom.el("a", {
          className: "tag",
          text: genre.name || genre.slug,
          href: self.urlService.pageUrl("rankings/genre-ranking.html?genre=" + encodeURIComponent(genre.slug))
        }));
      });
    }

    renderLinks(container) {
      var self = this;
      this.dom.clear(container);
      this.genreCatalog.visibleGenres().forEach(function (genre) {
        var link = self.dom.el("a", {
          className: "genre-link",
          href: self.urlService.pageUrl("rankings/genre-ranking.html?genre=" + encodeURIComponent(genre.slug))
        });
        link.appendChild(self.dom.el("strong", { text: genre.name }));
        link.appendChild(self.dom.el("span", { text: genre.slug }));
        container.appendChild(link);
      });
    }
  }

  window.YTRankClasses.GenreLinkRenderer = GenreLinkRenderer;
})();
