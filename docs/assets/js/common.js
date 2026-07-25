(function () {
  "use strict";

  class YTRankBootstrap {
    constructor(dependencies) {
      this.document = dependencies.document;
      this.window = dependencies.window;
      this.classes = dependencies.classes;
    }

    createApp() {
      var genres = new this.classes.GenreCatalog([
        { slug: "gaming", name: "ゲーム" },
        { slug: "minecraft", name: "Minecraft" },
        { slug: "music", name: "音楽" },
        { slug: "programming", name: "IT・プログラミング" },
        { slug: "learning", name: "学習・情報" },
        { slug: "uncategorized", name: "未分類" }
      ]);
      var dom = new this.classes.DomService(this.document);
      var urlService = new this.classes.UrlService({ document: this.document });
      var formatter = new this.classes.Formatter();
      var repository = new this.classes.JsonRepository({
        urlService: urlService,
        fetch: this.window.fetch.bind(this.window)
      });
      var healthService = new this.classes.GenerationHealthService(formatter);
      var rankChangePresenter = new this.classes.RankChangePresenter();
      var youtubeUrlService = new this.classes.YouTubeUrlService();
      var genreRenderer = new this.classes.GenreLinkRenderer({
        dom: dom,
        urlService: urlService,
        genreCatalog: genres
      });
      var cardRenderer = new this.classes.RankingCardRenderer({
        dom: dom,
        formatter: formatter,
        urlService: urlService,
        genreRenderer: genreRenderer,
        rankChangePresenter: rankChangePresenter,
        youtubeUrlService: youtubeUrlService
      });
      var listRenderer = new this.classes.RankingListRenderer({
        dom: dom,
        cardRenderer: cardRenderer
      });

      new this.classes.ThemeController({
        document: this.document,
        window: this.window,
        dom: dom,
        storage: this.window.localStorage
      }).init();

      return new this.classes.YTRankApplication({
        dom: dom,
        urlService: urlService,
        formatter: formatter,
        repository: repository,
        healthService: healthService,
        genreCatalog: genres,
        genreRenderer: genreRenderer,
        listRenderer: listRenderer,
        rankChangePresenter: rankChangePresenter,
        youtubeUrlService: youtubeUrlService
      });
    }

    start() {
      this.window.YTRank = this.createApp();
    }
  }

  new YTRankBootstrap({
    document: document,
    window: window,
    classes: window.YTRankClasses
  }).start();
})();
