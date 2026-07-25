(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class RankChangePresenter {
    label(entry) {
      if (entry.previousRank === null || entry.previousRank === undefined) {
        return { text: "NEW", className: "rank-change new" };
      }
      if (!entry.rankChange) return { text: "-", className: "rank-change" };
      if (entry.rankChange > 0) return { text: "↑ " + entry.rankChange, className: "rank-change up" };
      return { text: "↓ " + Math.abs(entry.rankChange), className: "rank-change down" };
    }
  }

  window.YTRankClasses.RankChangePresenter = RankChangePresenter;
})();
