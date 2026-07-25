(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class JsonRepository {
    constructor(dependencies) {
      this.urlService = dependencies.urlService;
      this.fetch = dependencies.fetch;
    }

    load(path) {
      return this.fetch(this.urlService.dataUrl(path), { cache: "no-store" }).then(function (response) {
        if (!response.ok) throw new Error("JSONを読み込めませんでした");
        return response.json();
      });
    }
  }

  window.YTRankClasses.JsonRepository = JsonRepository;
})();
