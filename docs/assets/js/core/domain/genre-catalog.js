(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class GenreCatalog {
    constructor(genres) {
      this.items = genres;
    }

    replace(genres) {
      this.items = genres;
    }

    visibleGenres() {
      return this.items.filter(function (genre) {
        return genre.slug !== "uncategorized";
      });
    }
  }

  window.YTRankClasses.GenreCatalog = GenreCatalog;
})();
