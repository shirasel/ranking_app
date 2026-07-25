(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class GenerationHealthService {
    constructor(formatter) {
      this.formatter = formatter;
    }

    freshness(value) {
      if (!value) {
        return { text: "日時不明", detail: "生成日時を確認できません。", level: "stale" };
      }

      var date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return { text: "日時不明", detail: "生成日時を確認できません。", level: "stale" };
      }

      var ageHours = Math.max(0, (Date.now() - date.getTime()) / 36e5);
      var ageText = ageHours < 1
        ? Math.max(0, Math.round(ageHours * 60)) + "分前"
        : Math.round(ageHours) + "時間前";

      if (ageHours <= 8) return { text: "最新", detail: "生成 " + ageText, level: "ok" };
      if (ageHours <= 24) return { text: "要確認", detail: "生成 " + ageText, level: "warn" };
      return { text: "古いデータ", detail: "生成 " + ageText, level: "stale" };
    }

    health(summary) {
      var collection = summary.collection || {};
      var sourceResults = collection.sourceResults || [];
      var checks = [];
      var freshness = this.freshness(summary.generatedAt);
      var skippedSources = sourceResults.filter(function (source) {
        return source.status === "skipped";
      });

      checks.push({ level: freshness.level, title: "生成時刻", detail: freshness.detail });
      checks.push((summary.rankingVideos || 0) < 1
        ? { level: "stale", title: "ランキング反映", detail: "ランキング対象動画が0本です。" }
        : { level: "ok", title: "ランキング反映", detail: this.formatter.formatNumber(summary.rankingVideos || 0) + "本を表示できます。" });
      checks.push(skippedSources.length > 0
        ? { level: "warn", title: "収集元", detail: this.formatter.formatNumber(skippedSources.length) + "件の収集元がスキップされました。" }
        : { level: "ok", title: "収集元", detail: "スキップされた収集元はありません。" });
      checks.push((summary.inputVideos || 0) > (collection.publicVideos || 0)
        ? { level: "warn", title: "公開動画", detail: "非公開または削除済みの動画が含まれている可能性があります。" }
        : { level: "ok", title: "公開動画", detail: "入力動画はすべて公開動画として扱われています。" });
      checks.push((collection.estimatedQuotaUnits || 0) >= 8000
        ? { level: "warn", title: "API使用量", detail: "推定使用量が高めです。収集元数を確認してください。" }
        : { level: "ok", title: "API使用量", detail: "推定使用量は通常範囲です。" });

      return checks;
    }
  }

  window.YTRankClasses.GenerationHealthService = GenerationHealthService;
})();
