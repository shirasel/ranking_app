(function () {
  "use strict";

  var app = window.YTRank;
  var page = document.body.dataset.page;

  if (page === "home") {
    initHome();
  } else if (page === "overall") {
    initOverall();
  } else if (page === "genre") {
    initGenre();
  }

  function initHome() {
    var genreLinks = app.qs("[data-genre-links]");
    if (genreLinks) app.renderGenreLinks(genreLinks);

    app.showState("overall", "ランキングJSONを読み込んでいます。");
    app.loadJson("latest/overall.json")
      .then(function (document) {
        app.showState("overall", "");
        app.setText("[data-updated-at]", app.formatDateTime(document.generatedAt));
        app.setText("[data-total-count]", app.formatNumber((document.ranking || []).length) + "本");
        app.renderRankingList(app.qs("[data-home-overall]"), (document.ranking || []).slice(0, 5), { compact: true });
      })
      .catch(function () {
        app.showState("overall", "まだランキングデータがありません。モック生成後に表示されます。");
        app.setText("[data-updated-at]", "未生成");
      });

    loadPreview("latest/trending.json", "trending", "[data-home-trending]", "[data-trending-title]");
    loadPreview("latest/discovery.json", "discovery", "[data-home-discovery]", "[data-discovery-title]");
    loadGenerationSummary();
  }

  function loadGenerationSummary() {
    app.showState("summary", "生成サマリーを読み込んでいます。");
    app.loadJson("latest/generation-summary.json")
      .then(function (summary) {
        app.showState("summary", "");
        renderGenerationSummary(summary);
      })
      .catch(function () {
        app.showState("summary", "生成サマリーはまだありません。");
        app.setText("[data-summary-status]", "未生成");
      });
  }

  function renderGenerationSummary(summary) {
    var collection = summary.collection || {};
    var retention = summary.retention || {};
    var sourceResults = collection.sourceResults || [];
    var freshness = app.generationFreshness(summary.generatedAt);
    var skippedCount = sourceResults.filter(function (result) {
      return result.status === "skipped";
    }).length;
    var statusText = freshness.text;
    var statusNode = app.qs("[data-summary-status]");
    if (skippedCount > 0 && freshness.level === "ok") {
      statusText = "一部スキップ";
    }

    app.setText("[data-summary-status]", statusText);
    if (statusNode) statusNode.className = "status-pill summary-status " + freshness.level;
    app.setText("[data-summary-input]", app.formatNumber(summary.inputVideos || 0) + "本");
    app.setText("[data-summary-public]", app.formatNumber(collection.publicVideos || 0) + "本");
    app.setText("[data-summary-ranking]", app.formatNumber(summary.rankingVideos || 0) + "本");
    app.setText("[data-summary-quota]", app.formatNumber(collection.estimatedQuotaUnits || 0) + " units");
    app.setText(
      "[data-summary-retention]",
      "保持期間チェック: 履歴削除 " + app.formatNumber(retention.historyDeleted || 0)
        + "件 / 詳細削除 " + app.formatNumber(retention.videoDetailsDeleted || 0) + "件"
    );
    app.setText("[data-summary-freshness]", freshness.detail);

    renderSourceResults(sourceResults);
  }

  function renderSourceResults(sourceResults) {
    var container = app.qs("[data-summary-sources]");
    if (!container) return;
    app.clear(container);

    if (!sourceResults.length) {
      container.appendChild(app.el("p", { className: "muted-text", text: "収集元記録なし" }));
      return;
    }

    sourceResults.slice(0, 8).forEach(function (result) {
      var row = app.el("article", { className: "source-row" });
      var main = app.el("div", { className: "source-main" });
      main.appendChild(app.el("strong", { text: result.source || "source" }));
      main.appendChild(app.el("span", {
        text: app.formatNumber(result.collected || 0) + " / " + app.formatNumber(result.requested || 0) + "件"
      }));
      row.appendChild(main);
      row.appendChild(app.el("span", {
        className: "source-status " + (result.status === "skipped" ? "skipped" : "ok"),
        text: result.status === "skipped" ? "Skipped" : "OK"
      }));
      container.appendChild(row);
    });
  }

  function loadPreview(path, stateName, listSelector, titleSelector) {
    app.showState(stateName, "読み込み中です。");
    app.loadJson(path)
      .then(function (document) {
        app.showState(stateName, "");
        var entries = (document.ranking || []).slice(0, 3);
        app.renderRankingList(app.qs(listSelector), entries, { compact: true });
        app.setText(titleSelector, entries[0] ? entries[0].title : "-");
      })
      .catch(function () {
        app.showState(stateName, "データ生成後に表示されます。");
      });
  }

  function initOverall() {
    var allEntries = [];
    var container = app.qs("[data-overall-ranking]");
    var input = app.qs("[data-ranking-search]");

    app.showState("overall", "ランキングJSONを読み込んでいます。");
    app.loadJson("latest/overall.json")
      .then(function (document) {
        app.showState("overall", "");
        app.setText("[data-updated-at]", "最終更新 " + app.formatDateTime(document.generatedAt));
        allEntries = document.ranking || [];
        app.renderRankingList(container, allEntries);
      })
      .catch(function () {
        app.showState("overall", "ランキングデータを読み込めませんでした。");
      });

    if (input) {
      input.addEventListener("input", function () {
        var query = input.value.trim().toLowerCase();
        var filtered = allEntries.filter(function (entry) {
          var genreText = (entry.genres || []).map(function (genre) {
            return genre.name + " " + genre.slug;
          }).join(" ");
          return (entry.title + " " + entry.channelName + " " + genreText).toLowerCase().indexOf(query) >= 0;
        });
        app.renderRankingList(container, filtered);
      });
    }
  }

  function initGenre() {
    var select = app.qs("[data-genre-select]");
    var selected = new URLSearchParams(window.location.search).get("genre") || "gaming";

    app.GENRES.forEach(function (genre) {
      var option = app.el("option", { text: genre.name, value: genre.slug });
      if (genre.slug === selected) option.selected = true;
      select.appendChild(option);
    });

    select.addEventListener("change", function () {
      var url = new URL(window.location.href);
      url.searchParams.set("genre", select.value);
      window.location.href = url.toString();
    });

    loadGenre(selected);
  }

  function loadGenre(slug) {
    var container = app.qs("[data-genre-ranking]");
    app.showState("genre", "ジャンルランキングJSONを読み込んでいます。");
    app.loadJson("latest/genres/" + encodeURIComponent(slug) + ".json")
      .then(function (document) {
        app.showState("genre", "");
        app.setText("[data-genre-title]", (document.genre && document.genre.name ? document.genre.name : slug) + "ランキング");
        app.setText("[data-updated-at]", "最終更新 " + app.formatDateTime(document.generatedAt));
        app.setText("[data-genre-status]", document.status === "official" ? "正式ランキング" : "参考順位");
        app.renderRankingList(container, document.ranking || []);
      })
      .catch(function () {
        app.setText("[data-genre-title]", "ジャンル別ランキング");
        app.setText("[data-updated-at]", "未生成");
        app.setText("[data-genre-status]", "集計中");
        app.showState("genre", "このジャンルのランキングデータはまだありません。");
        app.clear(container);
      });
  }
})();
