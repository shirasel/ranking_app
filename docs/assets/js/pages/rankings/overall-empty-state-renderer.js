(function () {
  "use strict";

  class OverallEmptyStateRenderer {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.container = dependencies.container;
      this.labels = dependencies.labels;
    }

    render(path, view, query) {
      if (query) {
        this.app.renderEmptyState(this.container, {
          title: "検索条件に一致する動画がありません。",
          message: this.labels.periodLabel(path, view) + "ランキング内で、別のキーワードを試してください。"
        });
        return;
      }

      if (path.indexOf("today.json") >= 0) {
        this.app.renderEmptyState(this.container, {
          title: "本日分の増加データはまだありません。",
          message: "本日ランキングは、今日の複数回の収集結果から再生数の増加が確認できた動画だけを表示します。",
          actionText: "24時間ランキングを見る",
          href: "overall-ranking.html"
        });
        return;
      }

      this.app.renderEmptyState(this.container, {
        title: this.labels.periodLabel(path, view) + "ランキングはまだありません。",
        message: "次回のランキング生成後に表示されます。"
      });
    }
  }

  window.YTRankPages = window.YTRankPages || {};
  window.YTRankPages.OverallEmptyStateRenderer = OverallEmptyStateRenderer;
})();
