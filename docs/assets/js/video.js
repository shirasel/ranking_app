(function () {
  "use strict";

  class VideoDetailPage {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.window = dependencies.window;
      this.container = this.app.qs("[data-video-detail]");
      this.videoId = new URLSearchParams(this.window.location.search).get("id");
      this.renderer = new VideoDetailRenderer(this.app, this.container);
    }

    init() {
      if (!this.videoId) {
        this.app.showState("video", "動画IDが指定されていません。");
        return;
      }
      if (!/^[A-Za-z0-9_-]{6,64}$/.test(this.videoId)) {
        this.app.showState("video", "動画IDの形式が正しくありません。");
        return;
      }
      this.loadVideo(this.videoId);
    }

    loadVideo(videoId) {
      var app = this.app;
      var renderer = this.renderer;
      app.showState("video", "動画詳細JSONを読み込んでいます。");
      Promise.all([
        app.loadJson("videos/" + encodeURIComponent(videoId) + ".json"),
        app.loadJson("statistics/videos/" + encodeURIComponent(videoId) + ".json").catch(function () { return null; }),
        app.loadJson("rankings/videos/" + encodeURIComponent(videoId) + ".json").catch(function () { return null; })
      ])
        .then(function (results) {
          app.showState("video", "");
          renderer.render(results[0], results[1], results[2]);
        })
        .catch(function () {
          app.showState("video", "動画詳細データを読み込めませんでした。");
        });
    }
  }

  class VideoDetailRenderer {
    constructor(app, container) {
      this.app = app;
      this.container = container;
      this.metricRenderer = new MetricRenderer(app);
      this.scoreRenderer = new ScoreBreakdownRenderer(app);
      this.statisticsRenderer = new StatisticsHistoryRenderer(app);
      this.rankingHistoryRenderer = new RankingHistoryRenderer(app);
    }

    render(document, history, rankHistory) {
      var app = this.app;
      var entry = document.video;
      app.clear(this.container);
      app.setText("[data-video-title]", entry.title);
      app.setText("[data-updated-at]", "最終更新 " + app.formatDateTime(document.generatedAt));

      var left = app.el("div");
      left.appendChild(this.createMedia(entry));
      left.appendChild(this.createScorePanel(entry.scoreBreakdown || {}));
      left.appendChild(this.rankingHistoryRenderer.create(rankHistory));
      left.appendChild(this.statisticsRenderer.create(history));
      this.container.appendChild(left);
      this.container.appendChild(this.createSidePanel(entry));
    }

    createMedia(entry) {
      var app = this.app;
      var media = app.el("section", { className: "detail-media" });
      var thumb = app.el("div", { className: "thumbnail" });
      if (entry.thumbnailUrl) {
        thumb.appendChild(app.el("img", { src: entry.thumbnailUrl, alt: entry.title + " のサムネイル" }));
      }
      media.appendChild(thumb);
      media.appendChild(app.el("h2", { text: entry.title }));

      var meta = app.el("div", { className: "video-meta" });
      meta.appendChild(app.el("span", { text: entry.channelName }));
      meta.appendChild(app.el("span", { text: app.formatDateTime(entry.publishedAt) }));
      media.appendChild(meta);

      var tags = app.el("div", { className: "genre-tags" });
      app.appendGenreTags(tags, entry.genres);
      media.appendChild(tags);

      var youtubeUrl = app.safeYouTubeUrl(entry.videoId);
      if (youtubeUrl) {
        media.appendChild(app.el("a", { className: "button primary", text: "YouTubeで見る", href: youtubeUrl }));
      }
      return media;
    }

    createSidePanel(entry) {
      var app = this.app;
      var side = app.el("aside", { className: "detail-panel" });
      side.appendChild(app.el("h2", { text: "ランキング指標" }));
      side.appendChild(this.metricRenderer.create(entry));
      return side;
    }

    createScorePanel(breakdown) {
      var app = this.app;
      var panel = app.el("section", { className: "history-panel" });
      panel.appendChild(app.el("h2", { text: "スコア内訳" }));
      panel.appendChild(this.scoreRenderer.create(breakdown));
      return panel;
    }
  }

  class MetricRenderer {
    constructor(app) {
      this.app = app;
    }

    create(entry) {
      var app = this.app;
      var dl = app.el("dl");
      this.add(dl, "総合順位", entry.rank + "位");
      this.add(dl, "前回順位", entry.previousRank ? entry.previousRank + "位" : "NEW");
      this.add(dl, "順位変動", app.rankChangeLabel(entry).text);
      this.add(dl, "総合スコア", app.formatScore(entry.normalizedScore));
      this.add(dl, "Raw score", app.formatScore(entry.rawScore));
      this.add(dl, "再生回数", app.formatNumber(entry.viewCount));
      this.add(dl, "再生増加", app.formatNumber(entry.viewIncrease));
      this.add(dl, "高評価", app.formatNumber(entry.likeCount));
      this.add(dl, "コメント", app.formatNumber(entry.commentCount));
      this.add(dl, "登録者", app.formatNumber(entry.subscriberCount));
      return dl;
    }

    add(dl, label, value) {
      dl.appendChild(this.app.el("dt", { text: label }));
      dl.appendChild(this.app.el("dd", { text: value }));
    }
  }

  class ScoreBreakdownRenderer {
    constructor(app) {
      this.app = app;
    }

    create(breakdown) {
      var app = this.app;
      var list = app.el("div", { className: "bar-list" });
      [
        ["勢い", breakdown.velocity],
        ["反応率", breakdown.engagement],
        ["登録者比", breakdown.subscriberRatio],
        ["鮮度", breakdown.freshness]
      ].forEach(function (item) {
        var value = Number(item[1] || 0);
        var row = app.el("div", { className: "bar-row" });
        var label = app.el("div", { className: "bar-label" });
        label.appendChild(app.el("span", { text: item[0] }));
        label.appendChild(app.el("span", { text: app.formatScore(value) }));
        var track = app.el("div", { className: "bar-track" });
        var fill = app.el("div", { className: "bar-fill" });
        fill.style.width = Math.max(0, Math.min(100, value)) + "%";
        track.appendChild(fill);
        row.appendChild(label);
        row.appendChild(track);
        list.appendChild(row);
      });
      return list;
    }
  }

  class StatisticsHistoryRenderer {
    constructor(app) {
      this.app = app;
    }

    create(history) {
      var statistics = history && Array.isArray(history.statistics) ? history.statistics : [];
      if (!statistics.length) {
        return this.wrap("統計推移", this.app.el("p", { className: "muted-text", text: "統計履歴は次回以降の生成で蓄積されます。" }));
      }

      var wrapper = this.app.el("div", { className: "trend-table-wrap" });
      wrapper.appendChild(this.createTable(statistics));
      wrapper.appendChild(this.createViewTrend(statistics));
      return this.wrap("統計推移", wrapper);
    }

    wrap(title, content) {
      var panel = this.app.el("section", { className: "history-panel" });
      panel.appendChild(this.app.el("h2", { text: title }));
      panel.appendChild(content);
      return panel;
    }

    createTable(statistics) {
      var app = this.app;
      var table = app.el("table", { className: "trend-table" });
      var thead = app.el("thead");
      var headerRow = app.el("tr");
      ["取得日時", "再生回数", "高評価", "コメント", "登録者"].forEach(function (label) {
        headerRow.appendChild(app.el("th", { text: label }));
      });
      thead.appendChild(headerRow);
      table.appendChild(thead);

      var tbody = app.el("tbody");
      statistics.slice(-10).reverse().forEach(function (row) {
        var tr = app.el("tr");
        tr.appendChild(app.el("td", { text: app.formatDateTime(row.capturedAt) }));
        tr.appendChild(app.el("td", { text: app.formatNumber(row.viewCount) }));
        tr.appendChild(app.el("td", { text: app.formatNumber(row.likeCount) }));
        tr.appendChild(app.el("td", { text: app.formatNumber(row.commentCount) }));
        tr.appendChild(app.el("td", { text: app.formatNumber(row.subscriberCount) }));
        tbody.appendChild(tr);
      });
      table.appendChild(tbody);
      return table;
    }

    createViewTrend(statistics) {
      var app = this.app;
      var values = statistics.map(function (row) { return Number(row.viewCount || 0); });
      var max = Math.max.apply(null, values.concat([1]));
      var chart = app.el("div", { className: "trend-bars", title: "再生回数推移" });
      statistics.slice(-14).forEach(function (row) {
        var value = Number(row.viewCount || 0);
        var bar = app.el("div", { className: "trend-bar" });
        bar.style.height = Math.max(8, Math.round((value / max) * 88)) + "px";
        bar.setAttribute("aria-label", app.formatDateTime(row.capturedAt) + " 再生 " + app.formatNumber(value));
        chart.appendChild(bar);
      });
      return chart;
    }
  }

  class RankingHistoryRenderer {
    constructor(app) {
      this.app = app;
    }

    create(history) {
      var rankings = history && Array.isArray(history.rankings) ? history.rankings : [];
      if (!rankings.length) {
        return this.wrap(this.app.el("p", { className: "muted-text", text: "順位履歴は次回以降の生成で蓄積されます。" }));
      }

      var wrapper = this.app.el("div", { className: "trend-table-wrap" });
      wrapper.appendChild(this.createChart(rankings));
      wrapper.appendChild(this.createTable(rankings));
      return this.wrap(wrapper);
    }

    wrap(content) {
      var panel = this.app.el("section", { className: "history-panel" });
      panel.appendChild(this.app.el("h2", { text: "順位推移" }));
      panel.appendChild(content);
      return panel;
    }

    createChart(rankings) {
      var app = this.app;
      var chart = app.el("div", { className: "rank-timeline", title: "総合順位推移" });
      rankings.slice(-14).forEach(function (row) {
        var point = app.el("div", { className: "rank-point" });
        point.style.height = Math.max(10, 96 - Math.min(90, Number(row.rank || 100))) + "px";
        point.setAttribute("aria-label", app.formatDateTime(row.capturedAt) + " " + row.rank + "位");
        point.appendChild(app.el("span", { text: row.rank }));
        chart.appendChild(point);
      });
      return chart;
    }

    createTable(rankings) {
      var app = this.app;
      var table = app.el("table", { className: "trend-table" });
      var thead = app.el("thead");
      var headerRow = app.el("tr");
      ["取得日時", "順位", "前回順位", "変動", "スコア"].forEach(function (label) {
        headerRow.appendChild(app.el("th", { text: label }));
      });
      thead.appendChild(headerRow);
      table.appendChild(thead);

      var tbody = app.el("tbody");
      rankings.slice(-10).reverse().forEach(function (row) {
        var tr = app.el("tr");
        tr.appendChild(app.el("td", { text: app.formatDateTime(row.capturedAt) }));
        tr.appendChild(app.el("td", { text: row.rank + "位" }));
        tr.appendChild(app.el("td", { text: row.previousRank ? row.previousRank + "位" : "NEW" }));
        tr.appendChild(app.el("td", { text: row.rankChange === null || row.rankChange === undefined ? "-" : String(row.rankChange) }));
        tr.appendChild(app.el("td", { text: app.formatScore(row.normalizedScore) }));
        tbody.appendChild(tr);
      });
      table.appendChild(tbody);
      return table;
    }
  }

  new VideoDetailPage({
    app: window.YTRank,
    window: window
  }).init();
})();
