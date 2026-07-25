(function () {
  "use strict";

  var app = window.YTRank;
  var id = new URLSearchParams(window.location.search).get("id");
  var container = app.qs("[data-video-detail]");

  if (!id) {
    app.showState("video", "動画IDが指定されていません。");
  } else if (!/^[A-Za-z0-9_-]{6,64}$/.test(id)) {
    app.showState("video", "動画IDの形式が正しくありません。");
  } else {
    loadVideo(id);
  }

  function loadVideo(videoId) {
    app.showState("video", "動画詳細JSONを読み込んでいます。");
    Promise.all([
      app.loadJson("videos/" + encodeURIComponent(videoId) + ".json"),
      app.loadJson("statistics/videos/" + encodeURIComponent(videoId) + ".json").catch(function () {
        return null;
      })
    ])
      .then(function (results) {
        var document = results[0];
        var history = results[1];
        app.showState("video", "");
        render(document, history);
      })
      .catch(function () {
        app.showState("video", "動画詳細データを読み込めませんでした。");
      });
  }

  function render(document, history) {
    var entry = document.video;
    app.clear(container);
    app.setText("[data-video-title]", entry.title);
    app.setText("[data-updated-at]", "最終更新 " + app.formatDateTime(document.generatedAt));

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

    var side = app.el("aside", { className: "detail-panel" });
    side.appendChild(app.el("h2", { text: "ランキング指標" }));
    var dl = app.el("dl");
    addMetric(dl, "総合順位", entry.rank + "位");
    addMetric(dl, "前回順位", entry.previousRank ? entry.previousRank + "位" : "NEW");
    addMetric(dl, "順位変動", app.rankChangeLabel(entry).text);
    addMetric(dl, "総合スコア", app.formatScore(entry.normalizedScore));
    addMetric(dl, "Raw score", app.formatScore(entry.rawScore));
    addMetric(dl, "再生回数", app.formatNumber(entry.viewCount));
    addMetric(dl, "再生増加", app.formatNumber(entry.viewIncrease));
    addMetric(dl, "高評価", app.formatNumber(entry.likeCount));
    addMetric(dl, "コメント", app.formatNumber(entry.commentCount));
    addMetric(dl, "登録者", app.formatNumber(entry.subscriberCount));
    side.appendChild(dl);

    var bars = app.el("section", { className: "history-panel" });
    bars.appendChild(app.el("h2", { text: "スコア内訳" }));
    bars.appendChild(createBars(entry.scoreBreakdown || {}));

    var trends = app.el("section", { className: "history-panel" });
    trends.appendChild(app.el("h2", { text: "統計推移" }));
    trends.appendChild(createStatisticsHistory(history));

    var left = app.el("div");
    left.appendChild(media);
    left.appendChild(bars);
    left.appendChild(trends);
    container.appendChild(left);
    container.appendChild(side);
  }

  function addMetric(dl, label, value) {
    dl.appendChild(app.el("dt", { text: label }));
    dl.appendChild(app.el("dd", { text: value }));
  }

  function createBars(breakdown) {
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

  function createStatisticsHistory(history) {
    var statistics = history && Array.isArray(history.statistics) ? history.statistics : [];
    if (!statistics.length) {
      return app.el("p", { className: "muted-text", text: "統計履歴は次回以降の生成で蓄積されます。" });
    }

    var wrapper = app.el("div", { className: "trend-table-wrap" });
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
    wrapper.appendChild(table);

    var chart = createViewTrend(statistics);
    wrapper.appendChild(chart);
    return wrapper;
  }

  function createViewTrend(statistics) {
    var values = statistics.map(function (row) {
      return Number(row.viewCount || 0);
    });
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
})();
