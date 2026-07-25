(function () {
  "use strict";

  class RankingPageRouter {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.document = dependencies.document;
      this.window = dependencies.window;
      this.pages = dependencies.pages;
    }

    start() {
      var pageName = this.document.body.dataset.page;
      var PageClass = this.pages[pageName];
      if (!PageClass) return;
      new PageClass({
        app: this.app,
        document: this.document,
        window: this.window
      }).init();
    }
  }

  var pages = window.YTRankPages || {};
  new RankingPageRouter({
    app: window.YTRank,
    document: document,
    window: window,
    pages: {
      home: pages.HomeRankingPage,
      overall: pages.OverallRankingPage,
      genre: pages.GenreRankingPage,
      history: pages.HistoryRankingPage
    }
  }).start();
})();
