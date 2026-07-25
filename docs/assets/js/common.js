(function () {
  "use strict";

  var GENRES = [
    { slug: "gaming", name: "ゲーム" },
    { slug: "minecraft", name: "Minecraft" },
    { slug: "music", name: "音楽" },
    { slug: "programming", name: "IT・プログラミング" },
    { slug: "learning", name: "学習・情報" },
    { slug: "uncategorized", name: "未分類" }
  ];

  function pageDepthPrefix() {
    var page = document.body.dataset.page;
    return page === "home" ? "" : "../";
  }

  function dataUrl(path) {
    return pageDepthPrefix() + "data/" + path.replace(/^\/+/, "");
  }

  function pageUrl(path) {
    return pageDepthPrefix() + path.replace(/^\/+/, "");
  }

  function qs(selector, root) {
    return (root || document).querySelector(selector);
  }

  function qsa(selector, root) {
    return Array.prototype.slice.call((root || document).querySelectorAll(selector));
  }

  function el(tag, options) {
    var node = document.createElement(tag);
    options = options || {};
    if (options.className) node.className = options.className;
    if (options.text !== undefined) node.textContent = String(options.text);
    if (options.href) node.setAttribute("href", options.href);
    if (options.src) node.setAttribute("src", options.src);
    if (options.alt !== undefined) node.setAttribute("alt", options.alt);
    if (options.type) node.setAttribute("type", options.type);
    if (options.title) node.setAttribute("title", options.title);
    if (options.value !== undefined) node.value = options.value;
    if (options.disabled) node.disabled = true;
    return node;
  }

  function clear(node) {
    if (!node) return;
    while (node.firstChild) node.removeChild(node.firstChild);
  }

  function setText(selector, value) {
    var node = qs(selector);
    if (node) node.textContent = value;
  }

  function showState(name, message) {
    var node = qs('[data-state="' + name + '"]');
    if (!node) return;
    clear(node);
    if (!message) return;
    node.appendChild(el("div", { className: "state", text: message }));
  }

  function formatDateTime(value) {
    if (!value) return "-";
    var date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat("ja-JP", {
      dateStyle: "medium",
      timeStyle: "short"
    }).format(date);
  }

  function formatNumber(value) {
    if (value === null || value === undefined) return "-";
    return new Intl.NumberFormat("ja-JP").format(value);
  }

  function formatScore(value) {
    if (value === null || value === undefined) return "-";
    return Number(value).toFixed(1);
  }

  function generationFreshness(value) {
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

    if (ageHours <= 8) {
      return { text: "最新", detail: "生成 " + ageText, level: "ok" };
    }
    if (ageHours <= 24) {
      return { text: "要確認", detail: "生成 " + ageText, level: "warn" };
    }
    return { text: "古いデータ", detail: "生成 " + ageText, level: "stale" };
  }

  function generationHealth(summary) {
    var collection = summary.collection || {};
    var sourceResults = collection.sourceResults || [];
    var checks = [];
    var freshness = generationFreshness(summary.generatedAt);
    var skippedSources = sourceResults.filter(function (source) {
      return source.status === "skipped";
    });

    checks.push({
      level: freshness.level,
      title: "生成時刻",
      detail: freshness.detail
    });

    if ((summary.rankingVideos || 0) < 1) {
      checks.push({
        level: "stale",
        title: "ランキング反映",
        detail: "ランキング対象動画が0本です。"
      });
    } else {
      checks.push({
        level: "ok",
        title: "ランキング反映",
        detail: formatNumber(summary.rankingVideos || 0) + "本を表示できます。"
      });
    }

    if (skippedSources.length > 0) {
      checks.push({
        level: "warn",
        title: "収集元",
        detail: formatNumber(skippedSources.length) + "件の収集元がスキップされました。"
      });
    } else {
      checks.push({
        level: "ok",
        title: "収集元",
        detail: "スキップされた収集元はありません。"
      });
    }

    if ((summary.inputVideos || 0) > (collection.publicVideos || 0)) {
      checks.push({
        level: "warn",
        title: "公開動画",
        detail: "非公開または削除済みの動画が含まれている可能性があります。"
      });
    } else {
      checks.push({
        level: "ok",
        title: "公開動画",
        detail: "入力動画はすべて公開動画として扱われています。"
      });
    }

    if ((collection.estimatedQuotaUnits || 0) >= 8000) {
      checks.push({
        level: "warn",
        title: "API使用量",
        detail: "推定使用量が高めです。収集元数を確認してください。"
      });
    } else {
      checks.push({
        level: "ok",
        title: "API使用量",
        detail: "推定使用量は通常範囲です。"
      });
    }

    return checks;
  }

  function rankChangeLabel(entry) {
    if (entry.previousRank === null || entry.previousRank === undefined) {
      return { text: "NEW", className: "rank-change new" };
    }
    if (!entry.rankChange) return { text: "-", className: "rank-change" };
    if (entry.rankChange > 0) return { text: "↑ " + entry.rankChange, className: "rank-change up" };
    return { text: "↓ " + Math.abs(entry.rankChange), className: "rank-change down" };
  }

  function safeYouTubeUrl(videoId) {
    if (!/^[A-Za-z0-9_-]{6,20}$/.test(videoId || "")) return null;
    return "https://www.youtube.com/watch?v=" + encodeURIComponent(videoId);
  }

  function loadJson(path) {
    return fetch(dataUrl(path), { cache: "no-store" }).then(function (response) {
      if (!response.ok) {
        throw new Error("JSONを読み込めませんでした");
      }
      return response.json();
    });
  }

  function applyTheme() {
    var saved = localStorage.getItem("yt-rank-theme");
    var dark = saved ? saved === "dark" : window.matchMedia("(prefers-color-scheme: dark)").matches;
    document.documentElement.dataset.theme = dark ? "dark" : "light";
  }

  function setupThemeToggle() {
    applyTheme();
    qsa("[data-theme-toggle]").forEach(function (button) {
      button.addEventListener("click", function () {
        var next = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
        document.documentElement.dataset.theme = next;
        localStorage.setItem("yt-rank-theme", next);
      });
    });
  }

  function appendGenreTags(container, genres) {
    (genres || []).forEach(function (genre) {
      var tag = el("a", {
        className: "tag",
        text: genre.name || genre.slug,
        href: pageUrl("rankings/genre-ranking.html?genre=" + encodeURIComponent(genre.slug))
      });
      container.appendChild(tag);
    });
  }

  function createRankingCard(entry, options) {
    options = options || {};
    var card = el("article", { className: "ranking-card" });

    card.appendChild(el("div", { className: "rank-number", text: entry.rank }));

    var thumb = el("a", {
      className: "thumbnail",
      href: pageUrl("videos/video-detail.html?id=" + encodeURIComponent(entry.videoId))
    });
    if (entry.thumbnailUrl) {
      thumb.appendChild(el("img", { src: entry.thumbnailUrl, alt: entry.title + " のサムネイル" }));
    }
    card.appendChild(thumb);

    var main = el("div", { className: "video-main" });
    var title = el("h3", { className: "video-title" });
    title.appendChild(el("a", {
      text: entry.title,
        href: pageUrl("videos/video-detail.html?id=" + encodeURIComponent(entry.videoId))
    }));
    main.appendChild(title);

    var meta = el("div", { className: "video-meta" });
    meta.appendChild(el("span", { text: entry.channelName }));
    meta.appendChild(el("span", { text: "再生 " + formatNumber(entry.viewCount) }));
    meta.appendChild(el("span", { text: "増加 " + formatNumber(entry.viewIncrease) }));
    meta.appendChild(el("span", { text: formatDateTime(entry.publishedAt) }));
    main.appendChild(meta);

    var tags = el("div", { className: "genre-tags" });
    appendGenreTags(tags, entry.genres);
    main.appendChild(tags);
    card.appendChild(main);

    var extra = el("div", { className: "ranking-extra" });
    extra.appendChild(el("div", { className: "score", text: formatScore(entry.normalizedScore) }));
    var change = rankChangeLabel(entry);
    extra.appendChild(el("span", { className: change.className, text: change.text }));
    if (!options.compact) {
      var scoreGrid = el("div", { className: "score-grid" });
      scoreGrid.appendChild(el("span", { text: "勢い " + formatScore(entry.scoreBreakdown && entry.scoreBreakdown.velocity) }));
      scoreGrid.appendChild(el("span", { text: "反応 " + formatScore(entry.scoreBreakdown && entry.scoreBreakdown.engagement) }));
      scoreGrid.appendChild(el("span", { text: "登録者比 " + formatScore(entry.scoreBreakdown && entry.scoreBreakdown.subscriberRatio) }));
      scoreGrid.appendChild(el("span", { text: "鮮度 " + formatScore(entry.scoreBreakdown && entry.scoreBreakdown.freshness) }));
      extra.appendChild(scoreGrid);
    }

    var youtubeUrl = safeYouTubeUrl(entry.videoId);
    if (youtubeUrl && !options.compact) {
      extra.appendChild(el("a", { className: "text-link", text: "YouTube", href: youtubeUrl }));
    }

    card.appendChild(extra);
    return card;
  }

  function renderRankingList(container, entries, options) {
    clear(container);
    (entries || []).forEach(function (entry) {
      container.appendChild(createRankingCard(entry, options));
    });
  }

  function renderGenreLinks(container) {
    clear(container);
    GENRES.filter(function (genre) {
      return genre.slug !== "uncategorized";
    }).forEach(function (genre) {
      var link = el("a", {
        className: "genre-link",
        href: pageUrl("rankings/genre-ranking.html?genre=" + encodeURIComponent(genre.slug))
      });
      link.appendChild(el("strong", { text: genre.name }));
      link.appendChild(el("span", { text: genre.slug }));
      container.appendChild(link);
    });
  }

  setupThemeToggle();

  window.YTRank = {
    GENRES: GENRES,
    qs: qs,
    qsa: qsa,
    el: el,
    clear: clear,
    setText: setText,
    showState: showState,
    dataUrl: dataUrl,
    pageUrl: pageUrl,
    loadJson: loadJson,
    formatDateTime: formatDateTime,
    formatNumber: formatNumber,
    formatScore: formatScore,
    generationFreshness: generationFreshness,
    generationHealth: generationHealth,
    rankChangeLabel: rankChangeLabel,
    safeYouTubeUrl: safeYouTubeUrl,
    renderRankingList: renderRankingList,
    renderGenreLinks: renderGenreLinks,
    appendGenreTags: appendGenreTags
  };
})();
