(function () {
  "use strict";

  class OverallHistorySelector {
    constructor(dependencies) {
      this.app = dependencies.app;
      this.select = dependencies.select;
      this.items = [];
    }

    setItems(items) {
      this.items = items;
      if (!this.select || this.select.children.length) return;
      var app = this.app;
      items.forEach(function (item) {
        this.select.appendChild(app.el("option", {
          text: item.date + " / " + app.formatNumber(item.totalVideos) + "本",
          value: item.path
        }));
      }, this);
    }

    selectedItem() {
      if (!this.select) return null;
      return this.items.find(function (item) {
        return item.path === this.select.value;
      }, this) || null;
    }

    selectInitial(initialDate) {
      var selectedItem = this.items.find(function (item) {
        return item.date === initialDate;
      }) || this.items[0];
      if (this.select && selectedItem) this.select.value = selectedItem.path;
      return selectedItem;
    }
  }

  window.YTRankPages = window.YTRankPages || {};
  window.YTRankPages.OverallHistorySelector = OverallHistorySelector;
})();
