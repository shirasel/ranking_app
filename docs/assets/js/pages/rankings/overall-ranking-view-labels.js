(function () {
  "use strict";

  class OverallRankingViewLabels {
    periodLabel(path, view) {
      if (path.indexOf("today.json") >= 0) return "本日";
      if (path.indexOf("seven-days.json") >= 0) return "7日間";
      if (path.indexOf("trending.json") >= 0) return "急上昇";
      if (path.indexOf("discovery.json") >= 0) return "発掘";
      if (view === "history") return "過去日";
      return "24時間";
    }

    titleFor(view) {
      if (view === "today") return "本日ランキング";
      if (view === "seven-days") return "7日間ランキング";
      if (view === "trending") return "急上昇ランキング";
      if (view === "discovery") return "発掘ランキング";
      if (view === "history") return "過去日ランキング";
      return "総合ランキング";
    }

    eyebrowFor(view) {
      if (view === "today") return "Today ranking";
      if (view === "seven-days") return "Seven day ranking";
      if (view === "trending") return "Trending ranking";
      if (view === "discovery") return "Discovery ranking";
      if (view === "history") return "History ranking";
      return "Overall ranking";
    }
  }

  window.YTRankPages = window.YTRankPages || {};
  window.YTRankPages.OverallRankingViewLabels = OverallRankingViewLabels;
})();
