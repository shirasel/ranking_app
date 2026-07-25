"use strict";

const fs = require("fs");

class YouTubeApiKeyLoader {
  constructor(dependencies) {
    this.fs = dependencies.fs;
    this.env = dependencies.env;
    this.envFile = dependencies.envFile;
  }

  load() {
    if (this.env.YOUTUBE_API_KEY) {
      return this.env.YOUTUBE_API_KEY.trim();
    }
    if (!this.fs.existsSync(this.envFile)) {
      return "";
    }

    const line = this.fs
      .readFileSync(this.envFile, "utf8")
      .split(/\r?\n/)
      .map((value) => value.trim())
      .find((value) => value.startsWith("YOUTUBE_API_KEY="));

    if (!line) return "";
    return line.slice("YOUTUBE_API_KEY=".length).trim().replace(/^["']|["']$/g, "");
  }
}

class YouTubeApiKeyChecker {
  constructor(dependencies) {
    this.keyLoader = dependencies.keyLoader;
    this.fetch = dependencies.fetch;
    this.output = dependencies.output;
  }

  async run() {
    const key = this.keyLoader.load();
    if (!key) {
      this.output.error("YOUTUBE_API_KEY missing");
      return 1;
    }

    const url = new URL("https://www.googleapis.com/youtube/v3/videos");
    url.searchParams.set("part", "id");
    url.searchParams.set("id", "dQw4w9WgXcQ");
    url.searchParams.set("key", key);

    const response = await this.fetch(url);
    this.output.log(`HTTP_STATUS:${response.status}`);

    if (response.ok) return 0;
    const body = await response.text();
    const reason = body.match(/"reason"\s*:\s*"([^"]+)"/);
    if (reason) this.output.log(`REASON:${reason[1]}`);
    return 1;
  }
}

new YouTubeApiKeyChecker({
  keyLoader: new YouTubeApiKeyLoader({
    fs,
    env: process.env,
    envFile: ".env",
  }),
  fetch,
  output: console,
}).run()
  .then((exitCode) => process.exit(exitCode))
  .catch((error) => {
    console.error(`${error.name}: ${error.message}`);
    process.exit(1);
  });
