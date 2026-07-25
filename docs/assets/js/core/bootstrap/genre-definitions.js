(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class GenreDefinitions {
    all() {
      return [
        { slug: "gaming", name: "ゲーム" },
        { slug: "minecraft", name: "Minecraft" },
        { slug: "music", name: "音楽" },
        { slug: "programming", name: "IT・プログラミング" },
        { slug: "learning", name: "学習・情報" },
        { slug: "uncategorized", name: "未分類" }
      ];
    }
  }

  window.YTRankClasses.GenreDefinitions = GenreDefinitions;
})();
