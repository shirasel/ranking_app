(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class ThemeController {
    constructor(dependencies) {
      this.document = dependencies.document;
      this.window = dependencies.window;
      this.dom = dependencies.dom;
      this.storage = dependencies.storage;
    }

    init() {
      this.apply();
      this.bindToggle();
    }

    apply() {
      var saved = this.storage.getItem("yt-rank-theme");
      var dark = saved ? saved === "dark" : this.window.matchMedia("(prefers-color-scheme: dark)").matches;
      this.document.documentElement.dataset.theme = dark ? "dark" : "light";
    }

    bindToggle() {
      var self = this;
      this.dom.qsa("[data-theme-toggle]").forEach(function (button) {
        button.addEventListener("click", function () {
          var next = self.document.documentElement.dataset.theme === "dark" ? "light" : "dark";
          self.document.documentElement.dataset.theme = next;
          self.storage.setItem("yt-rank-theme", next);
        });
      });
    }
  }

  window.YTRankClasses.ThemeController = ThemeController;
})();
