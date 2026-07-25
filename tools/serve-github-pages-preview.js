"use strict";

const http = require("http");
const fs = require("fs");
const path = require("path");

class ContentTypeResolver {
  constructor(types) {
    this.types = types;
  }

  resolve(filePath) {
    return this.types[path.extname(filePath)] || "application/octet-stream";
  }
}

class StaticFileResolver {
  constructor(dependencies) {
    this.path = dependencies.path;
    this.root = dependencies.root;
  }

  resolve(requestUrl) {
    const requestPath = decodeURIComponent(requestUrl.split("?")[0]);
    const relativePath = requestPath === "/" ? "/index.html" : requestPath;
    const filePath = this.path.normalize(this.path.join(this.root, relativePath));
    if (!filePath.startsWith(this.root)) {
      return { status: 403, filePath: null };
    }
    return { status: 200, filePath };
  }
}

class StaticFileServer {
  constructor(dependencies) {
    this.http = dependencies.http;
    this.fs = dependencies.fs;
    this.host = dependencies.host;
    this.port = dependencies.port;
    this.fileResolver = dependencies.fileResolver;
    this.contentTypeResolver = dependencies.contentTypeResolver;
    this.output = dependencies.output;
  }

  start() {
    const server = this.http.createServer(this.handleRequest.bind(this));
    server.listen(this.port, this.host, () => {
      this.output.log(`Docs preview: http://${this.host}:${this.port}/`);
    });
  }

  handleRequest(request, response) {
    const resolved = this.fileResolver.resolve(request.url);
    if (resolved.status === 403) {
      response.writeHead(403, { "content-type": "text/plain; charset=utf-8" });
      response.end("Forbidden");
      return;
    }

    this.fs.readFile(resolved.filePath, (error, data) => {
      if (error) {
        response.writeHead(404, { "content-type": "text/plain; charset=utf-8" });
        response.end("Not found");
        return;
      }

      response.writeHead(200, {
        "content-type": this.contentTypeResolver.resolve(resolved.filePath)
      });
      response.end(data);
    });
  }
}

const root = path.resolve(__dirname, "..", "docs");
new StaticFileServer({
  http,
  fs,
  host: "127.0.0.1",
  port: Number(process.env.PORT || 4173),
  fileResolver: new StaticFileResolver({ path, root }),
  contentTypeResolver: new ContentTypeResolver({
    ".html": "text/html; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".webp": "image/webp"
  }),
  output: console,
}).start();
