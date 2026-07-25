(function () {
  "use strict";

  class YTRankBootstrap {
    constructor(dependencies) {
      this.document = dependencies.document;
      this.window = dependencies.window;
      this.applicationFactory = dependencies.applicationFactory;
    }

    start() {
      this.window.YTRank = this.applicationFactory.create();
    }
  }

  var classes = window.YTRankClasses;
  new YTRankBootstrap({
    document: document,
    window: window,
    applicationFactory: new classes.YTRankApplicationFactory({
      document: document,
      window: window,
      classes: classes
    })
  }).start();
})();
