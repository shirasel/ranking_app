(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class RankingListRenderer {
    constructor(dependencies) {
      this.dom = dependencies.dom;
      this.cardRenderer = dependencies.cardRenderer;
    }

    render(container, entries, options) {
      var self = this;
      this.dom.clear(container);
      (entries || []).forEach(function (entry) {
        container.appendChild(self.cardRenderer.create(entry, options));
      });
    }

    renderEmpty(container, options) {
      this.dom.clear(container);
      options = options || {};
      var state = this.dom.el("div", { className: "empty-state" });
      state.appendChild(this.dom.el("strong", { text: options.title || "表示できるランキングがありません。" }));
      state.appendChild(this.dom.el("p", { text: options.message || "条件に一致する動画がありません。" }));
      if (options.href && options.actionText) {
        state.appendChild(this.dom.el("a", {
          className: "button secondary",
          text: options.actionText,
          href: this.cardRenderer.urlService.pageUrl(options.href)
        }));
      }
      container.appendChild(state);
    }
  }

  window.YTRankClasses.RankingListRenderer = RankingListRenderer;
})();
