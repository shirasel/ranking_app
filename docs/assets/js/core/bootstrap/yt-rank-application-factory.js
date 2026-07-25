(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class YTRankApplicationFactory {
    constructor(dependencies) {
      this.document = dependencies.document;
      this.window = dependencies.window;
      this.classes = dependencies.classes;
      this.genreDefinitions = dependencies.genreDefinitions || new this.classes.GenreDefinitions();
    }

    create() {
      var core = this.createCoreServices();
      var renderers = this.createRenderers(core);
      this.startThemeController(core.dom);

      return new this.classes.YTRankApplication({
        dom: core.dom,
        urlService: core.urlService,
        formatter: core.formatter,
        repository: core.repository,
        healthService: core.healthService,
        genreCatalog: core.genreCatalog,
        genreRenderer: renderers.genreRenderer,
        listRenderer: renderers.listRenderer,
        rankChangePresenter: core.rankChangePresenter,
        youtubeUrlService: core.youtubeUrlService
      });
    }

    createCoreServices() {
      var dom = new this.classes.DomService(this.document);
      var urlService = new this.classes.UrlService({ document: this.document });
      var formatter = new this.classes.Formatter();
      var repository = new this.classes.JsonRepository({
        urlService: urlService,
        fetch: this.window.fetch.bind(this.window)
      });

      return {
        dom: dom,
        urlService: urlService,
        formatter: formatter,
        repository: repository,
        healthService: new this.classes.GenerationHealthService(formatter),
        genreCatalog: new this.classes.GenreCatalog(this.genreDefinitions.all()),
        rankChangePresenter: new this.classes.RankChangePresenter(),
        youtubeUrlService: new this.classes.YouTubeUrlService()
      };
    }

    createRenderers(core) {
      var genreRenderer = new this.classes.GenreLinkRenderer({
        dom: core.dom,
        urlService: core.urlService,
        genreCatalog: core.genreCatalog
      });
      var cardRenderer = new this.classes.RankingCardRenderer({
        dom: core.dom,
        formatter: core.formatter,
        urlService: core.urlService,
        genreRenderer: genreRenderer,
        rankChangePresenter: core.rankChangePresenter,
        youtubeUrlService: core.youtubeUrlService
      });

      return {
        genreRenderer: genreRenderer,
        listRenderer: new this.classes.RankingListRenderer({
          dom: core.dom,
          cardRenderer: cardRenderer
        })
      };
    }

    startThemeController(dom) {
      new this.classes.ThemeController({
        document: this.document,
        window: this.window,
        dom: dom,
        storage: this.window.localStorage
      }).init();
    }
  }

  window.YTRankClasses.YTRankApplicationFactory = YTRankApplicationFactory;
})();
