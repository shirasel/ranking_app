(function () {
  "use strict";

  window.YTRankClasses = window.YTRankClasses || {};

  class DomService {
    constructor(documentRef) {
      this.document = documentRef;
    }

    qs(selector, root) {
      return (root || this.document).querySelector(selector);
    }

    qsa(selector, root) {
      return Array.prototype.slice.call((root || this.document).querySelectorAll(selector));
    }

    el(tag, options) {
      var node = this.document.createElement(tag);
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

    clear(node) {
      if (!node) return;
      while (node.firstChild) node.removeChild(node.firstChild);
    }

    setText(selector, value) {
      var node = this.qs(selector);
      if (node) node.textContent = value;
    }
  }

  window.YTRankClasses.DomService = DomService;
})();
