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
    app.loadJson("videos/" + encodeURIComponent(videoId) + ".json")
      .then(function (document) {
        app.showState("video", "");
        render(document);
      })
      .catch(function () {
        app.showState("video", "動画詳細データを読み込めませんでした。");
      });
  }

  function render(document) {
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

    var left = app.el("div");
    left.appendChild(media);
    left.appendChild(bars);
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
})();
