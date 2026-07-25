(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class GenreDefinitions {
    all() {
      return [
        { slug: "gaming", name: "ゲーム" },
        { slug: "entertainment", name: "エンタメ" },
        { slug: "music", name: "音楽" },
        { slug: "vtuber", name: "VTuber" },
        { slug: "learning", name: "学習・情報" },
        { slug: "cooking", name: "料理" },
        { slug: "gadgets", name: "ガジェット" },
        { slug: "travel", name: "旅行" },
        { slug: "beauty", name: "美容" },
        { slug: "sports", name: "スポーツ" },
        { slug: "anime", name: "アニメ" },
        { slug: "uncategorized", name: "未分類" }
      ];
    }
  }

  window.YTRankClasses.GenreDefinitions = GenreDefinitions;
})();
