(function () {
  "use strict";

  class OverallRankingUrlState {
    constructor(dependencies) {
      this.window = dependencies.window;
      this.selectedHistoryItem = dependencies.selectedHistoryItem;
      this.params = new URLSearchParams(this.window.location.search);
    }

    initialView() {
      return this.params.get("view") || "overall";
    }

    initialQuery() {
      return this.params.get("q") || "";
    }

    initialDate() {
      return this.params.get("date") || "";
    }

    update(view, query) {
      var url = new URL(this.window.location.href);
      if (view === "overall") {
        url.searchParams.delete("view");
      } else {
        url.searchParams.set("view", view);
      }

      if (query) {
        url.searchParams.set("q", query);
      } else {
        url.searchParams.delete("q");
      }

      if (view === "history") {
        var item = this.selectedHistoryItem();
        if (item && item.date) url.searchParams.set("date", item.date);
      } else {
        url.searchParams.delete("date");
      }

      this.window.history.replaceState(null, "", url.toString());
    }
  }

  window.YTRankPages = window.YTRankPages || {};
  window.YTRankPages.OverallRankingUrlState = OverallRankingUrlState;
})();
