"use strict";

const fs = require("fs");

function loadYoutubeApiKey() {
  if (process.env.YOUTUBE_API_KEY) {
    return process.env.YOUTUBE_API_KEY.trim();
  }

  if (!fs.existsSync(".env")) {
    return "";
  }

  const line = fs
    .readFileSync(".env", "utf8")
    .split(/\r?\n/)
    .map((value) => value.trim())
    .find((value) => value.startsWith("YOUTUBE_API_KEY="));

  if (!line) {
    return "";
  }

  return line
    .slice("YOUTUBE_API_KEY=".length)
    .trim()
    .replace(/^["']|["']$/g, "");
}

async function main() {
  const key = loadYoutubeApiKey();
  if (!key) {
    console.error("YOUTUBE_API_KEY missing");
    process.exit(1);
  }

  const url = new URL("https://www.googleapis.com/youtube/v3/videos");
  url.searchParams.set("part", "id");
  url.searchParams.set("id", "dQw4w9WgXcQ");
  url.searchParams.set("key", key);

  const response = await fetch(url);
  console.log(`HTTP_STATUS:${response.status}`);

  if (!response.ok) {
    const body = await response.text();
    const reason = body.match(/"reason"\s*:\s*"([^"]+)"/);
    if (reason) {
      console.log(`REASON:${reason[1]}`);
    }
    process.exit(1);
  }
}

main().catch((error) => {
  console.error(`${error.name}: ${error.message}`);
  process.exit(1);
});
