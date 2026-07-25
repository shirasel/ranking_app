(function () {
  "use strict";

  class RankingEntryFilter {
    filter(entries, query) {
      var normalizedQuery = (query || "").trim().toLowerCase();
      if (!normalizedQuery) return entries;

      return entries.filter(function (entry) {
        return searchableText(entry).indexOf(normalizedQuery) >= 0;
      });
    }
  }

  function searchableText(entry) {
    var genreText = (entry.genres || []).map(function (genre) {
      return genre.name + " " + genre.slug;
    }).join(" ");
    return (entry.title + " " + entry.channelName + " " + genreText).toLowerCase();
  }

  window.YTRankPages = window.YTRankPages || {};
  window.YTRankPages.RankingEntryFilter = RankingEntryFilter;
})();
