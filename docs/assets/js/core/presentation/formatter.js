(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class Formatter {
    formatDateTime(value) {
      if (!value) return "-";
      var date = new Date(value);
      if (Number.isNaN(date.getTime())) return value;
      return new Intl.DateTimeFormat("ja-JP", {
        dateStyle: "medium",
        timeStyle: "short"
      }).format(date);
    }

    formatNumber(value) {
      if (value === null || value === undefined) return "-";
      return new Intl.NumberFormat("ja-JP").format(value);
    }

    formatScore(value) {
      if (value === null || value === undefined) return "-";
      return Number(value).toFixed(1);
    }
  }

  window.YTRankClasses.Formatter = Formatter;
})();
