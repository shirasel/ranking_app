(function () {
  "use strict";

  class OperationJsonLoader {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.path = dependencies.path;
    }

    load() {
      return this.app.loadJson(this.path);
    }
  }

  window.YTRankOperation = window.YTRankOperation || {};
  window.YTRankOperation.OperationJsonLoader = OperationJsonLoader;
})();
