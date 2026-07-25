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
  } else if (page === "history") {
    initHistory();
  }

  function initHome() {
    var genreLinks = app.qs("[data-genre-links]");
    if (genreLinks) app.renderGenreLinks(genreLinks);

    app.showState("overall", "ランキングJSONを読み込んでいます。");
    app.loadJson("latest/overall.json")
      .then(function (document) {
        app.showState("overall", "");
        app.setText("[data-updated-at]", app.formatDateTime(document.generatedAt));
        app.renderRankingList(app.qs("[data-home-overall]"), (document.ranking || []).slice(0, 5), { compact: true });
      })
      .catch(function () {
        app.showState("overall", "まだランキングデータがありません。次回の実データ生成後に表示されます。");
        app.setText("[data-updated-at]", "未生成");
      });

    loadPreview("latest/trending.json", "trending", "[data-home-trending]");
    loadPreview("latest/discovery.json", "discovery", "[data-home-discovery]");
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

    renderHealthChecks("[data-summary-health]", app.generationHealth(summary).slice(0, 3));
    renderSourceResults(sourceResults);
  }

  function renderHealthChecks(selector, checks) {
    var container = app.qs(selector);
    if (!container) return;
    app.clear(container);

    checks.forEach(function (check) {
      var item = app.el("article", { className: "health-item " + check.level });
      item.appendChild(app.el("strong", { text: check.title }));
      item.appendChild(app.el("span", { text: check.detail }));
      container.appendChild(item);
    });
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

  function loadPreview(path, stateName, listSelector) {
    app.showState(stateName, "読み込み中です。");
    app.loadJson(path)
      .then(function (document) {
        app.showState(stateName, "");
        var entries = (document.ranking || []).slice(0, 3);
        app.renderRankingList(app.qs(listSelector), entries, { compact: true });
      })
      .catch(function () {
        app.showState(stateName, "次回の実データ生成後に表示されます。");
      });
  }

  function initOverall() {
    var allEntries = [];
    var container = app.qs("[data-overall-ranking]");
    var input = app.qs("[data-ranking-search]");
    var periodButtons = app.qsa("[data-period-button]");
    var viewButtons = app.qsa("[data-ranking-view]");
    var historyButton = app.qs("[data-history-button]");
    var historyPicker = app.qs("[data-history-picker]");
    var historySelect = app.qs("[data-overall-history-select]");
    var searchParams = new URLSearchParams(window.location.search);
    var initialView = searchParams.get("view") || "overall";
    var initialQuery = searchParams.get("q") || "";
    var initialDate = searchParams.get("date") || "";
    var currentPath = "latest/overall.json";
    var currentView = "overall";
    var historyItems = [];

    function periodLabel(path) {
      if (path.indexOf("today.json") >= 0) return "本日";
      if (path.indexOf("seven-days.json") >= 0) return "7日間";
      if (path.indexOf("trending.json") >= 0) return "急上昇";
      if (path.indexOf("discovery.json") >= 0) return "発掘";
      if (currentView === "history") return "過去日";
      return "24時間";
    }

    function titleForView(view) {
      if (view === "today") return "本日ランキング";
      if (view === "seven-days") return "7日間ランキング";
      if (view === "trending") return "急上昇ランキング";
      if (view === "discovery") return "発掘ランキング";
      if (view === "history") return "過去日ランキング";
      return "総合ランキング";
    }

    function eyebrowForView(view) {
      if (view === "today") return "Today ranking";
      if (view === "seven-days") return "Seven day ranking";
      if (view === "trending") return "Trending ranking";
      if (view === "discovery") return "Discovery ranking";
      if (view === "history") return "History ranking";
      return "Overall ranking";
    }

    function renderOverallEntries(entries, query) {
      app.showState("overall", "");
      if (entries.length) {
        app.renderRankingList(container, entries);
        return;
      }

      if (query) {
        app.renderEmptyState(container, {
          title: "検索条件に一致する動画がありません。",
          message: periodLabel(currentPath) + "ランキング内で、別のキーワードを試してください。"
        });
        return;
      }

      if (currentPath.indexOf("today.json") >= 0) {
        app.renderEmptyState(container, {
          title: "本日分の増加データはまだありません。",
          message: "本日ランキングは、今日の複数回の収集結果から再生数の増加が確認できた動画だけを表示します。",
          actionText: "24時間ランキングを見る",
          href: "rankings/overall-ranking.html"
        });
        return;
      }

      app.renderEmptyState(container, {
        title: periodLabel(currentPath) + "ランキングはまだありません。",
        message: "次回のランキング生成後に表示されます。"
      });
    }

    function setActiveButton(view) {
      viewButtons.forEach(function (item) {
        item.classList.toggle("active", item.getAttribute("data-ranking-view") === view);
      });
    }

    function selectedHistoryItem() {
      if (!historySelect) return null;
      return historyItems.find(function (item) {
        return item.path === historySelect.value;
      }) || null;
    }

    function updateViewUrl(view, query) {
      var url = new URL(window.location.href);
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
        var item = selectedHistoryItem();
        if (item && item.date) {
          url.searchParams.set("date", item.date);
        }
      } else {
        url.searchParams.delete("date");
      }
      window.history.replaceState(null, "", url.toString());
    }

    function toggleHistoryPicker(view) {
      if (!historyPicker) return;
      historyPicker.hidden = view !== "history";
    }

    function loadOverall(path, view, shouldUpdateUrl) {
      currentPath = path;
      currentView = view || "overall";
      setActiveButton(currentView);
      toggleHistoryPicker(currentView);
      app.setText("[data-ranking-eyebrow]", eyebrowForView(currentView));
      app.setText("[data-ranking-title]", titleForView(currentView));
      if (shouldUpdateUrl) updateViewUrl(currentView, input ? input.value.trim() : "");
      app.showState("overall", "ランキングJSONを読み込んでいます。");
      app.loadJson(path)
      .then(function (document) {
        app.setText("[data-updated-at]", "最終更新 " + app.formatDateTime(document.generatedAt));
        allEntries = document.ranking || [];
        renderFilteredEntries();
      })
      .catch(function () {
        app.showState("overall", "ランキングデータを読み込めませんでした。");
        app.clear(container);
      });
    }

    function loadOverallHistory(path, shouldUpdateUrl) {
      currentPath = path;
      currentView = "history";
      setActiveButton(currentView);
      toggleHistoryPicker(currentView);
      app.setText("[data-ranking-eyebrow]", eyebrowForView(currentView));
      app.setText("[data-ranking-title]", titleForView(currentView));
      if (shouldUpdateUrl) updateViewUrl(currentView, input ? input.value.trim() : "");
      app.showState("overall", "過去日ランキングJSONを読み込んでいます。");
      app.loadJson(path)
        .then(function (document) {
          app.setText("[data-updated-at]", "生成 " + app.formatDateTime(document.generatedAt));
          allEntries = document.ranking || [];
          renderFilteredEntries();
        })
        .catch(function () {
          app.showState("overall", "過去日ランキングを読み込めませんでした。");
          app.clear(container);
        });
    }

    function loadOverallHistoryIndex(shouldUpdateUrl) {
      setActiveButton("history");
      toggleHistoryPicker("history");
      app.setText("[data-ranking-eyebrow]", eyebrowForView("history"));
      app.setText("[data-ranking-title]", titleForView("history"));
      app.showState("overall", "履歴インデックスを読み込んでいます。");
      app.loadJson("latest/history-index.json")
        .then(function (index) {
          historyItems = (index.items || []).slice().reverse();
          if (!historyItems.length) {
            app.showState("overall", "過去日ランキングはまだありません。");
            app.clear(container);
            return;
          }

          if (historySelect && !historySelect.children.length) {
            historyItems.forEach(function (item) {
              historySelect.appendChild(app.el("option", {
                text: item.date + " / " + app.formatNumber(item.totalVideos) + "本",
                value: item.path
              }));
            });
          }

          var selectedItem = historyItems.find(function (item) {
            return item.date === initialDate;
          }) || historyItems[0];
          if (historySelect) historySelect.value = selectedItem.path;
          loadOverallHistory(selectedItem.path, shouldUpdateUrl);
        })
        .catch(function () {
          app.showState("overall", "履歴インデックスを読み込めませんでした。");
          app.clear(container);
        });
    }

    function renderFilteredEntries() {
      var query = input ? input.value.trim().toLowerCase() : "";
      var filtered = allEntries.filter(function (entry) {
        var genreText = (entry.genres || []).map(function (genre) {
          return genre.name + " " + genre.slug;
        }).join(" ");
        return (entry.title + " " + entry.channelName + " " + genreText).toLowerCase().indexOf(query) >= 0;
      });
      renderOverallEntries(filtered, query);
    }

    periodButtons.forEach(function (button) {
      button.addEventListener("click", function () {
        loadOverall(
          button.getAttribute("data-period-button"),
          button.getAttribute("data-ranking-view") || "overall",
          true
        );
      });
    });

    if (historyButton) {
      historyButton.addEventListener("click", function () {
        if (historyItems.length && historySelect && historySelect.value) {
          loadOverallHistory(historySelect.value, true);
          return;
        }
        loadOverallHistoryIndex(true);
      });
    }

    if (historySelect) {
      historySelect.addEventListener("change", function () {
        loadOverallHistory(historySelect.value, true);
      });
    }

    if (input) input.value = initialQuery;

    if (initialView === "history") {
      loadOverallHistoryIndex(false);
    } else {
      var initialButton = periodButtons.find(function (button) {
        return button.getAttribute("data-ranking-view") === initialView;
      }) || periodButtons[0];
      loadOverall(
        initialButton.getAttribute("data-period-button"),
        initialButton.getAttribute("data-ranking-view") || "overall",
        false
      );
    }

    if (input) {
      input.addEventListener("input", function () {
        var query = input.value.trim();
        updateViewUrl(currentView, query);
        renderFilteredEntries();
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

  function initHistory() {
    var select = app.qs("[data-history-select]");
    app.showState("history", "履歴インデックスを読み込んでいます。");
    app.loadJson("latest/history-index.json")
      .then(function (index) {
        var items = (index.items || []).slice().reverse();
        app.showState("history", "");
        if (!items.length) {
          app.showState("history", "過去日ランキングはまだありません。");
          return;
        }

        items.forEach(function (item) {
          select.appendChild(app.el("option", { text: item.date + " / " + app.formatNumber(item.totalVideos) + "本", value: item.path }));
        });

        var selectedDate = new URLSearchParams(window.location.search).get("date");
        var selectedItem = items.find(function (item) {
          return item.date === selectedDate;
        }) || items[0];
        select.value = selectedItem.path;
        select.addEventListener("change", function () {
          loadHistory(select.value);
        });
        loadHistory(selectedItem.path);
      })
      .catch(function () {
        app.showState("history", "履歴インデックスを読み込めませんでした。");
      });
  }

  function loadHistory(path) {
    var container = app.qs("[data-history-ranking]");
    app.showState("history", "過去日ランキングJSONを読み込んでいます。");
    app.loadJson(path)
      .then(function (document) {
        app.showState("history", "");
        app.setText("[data-history-title]", "過去日ランキング");
        app.setText("[data-updated-at]", "生成 " + app.formatDateTime(document.generatedAt));
        app.renderRankingList(container, document.ranking || []);
      })
      .catch(function () {
        app.showState("history", "過去日ランキングを読み込めませんでした。");
        app.clear(container);
      });
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
