const ownerRepo = process.env.GITHUB_REPOSITORY || "";
const token = process.env.GITHUB_TOKEN || "";
const workflow = process.env.GITHUB_WORKFLOW || "GitHub Actions";
const runId = process.env.GITHUB_RUN_ID || "";
const runAttempt = process.env.GITHUB_RUN_ATTEMPT || "1";
const refName = process.env.GITHUB_REF_NAME || "";
const sha = process.env.GITHUB_SHA || "";
const serverUrl = process.env.GITHUB_SERVER_URL || "https://github.com";

if (!ownerRepo || !token) {
  console.log("GitHub failure issue notification skipped outside GitHub Actions.");
  process.exit(0);
}

const [owner, repo] = ownerRepo.split("/");
const apiBase = "https://api.github.com";
const title = `[Actions failure] ${workflow}`;
const runUrl = `${serverUrl}/${ownerRepo}/actions/runs/${runId}`;
const body = [
  `Workflow failed: ${workflow}`,
  "",
  `Run: ${runUrl}`,
  `Attempt: ${runAttempt}`,
  `Branch: ${refName || "-"}`,
  `Commit: ${sha ? sha.slice(0, 12) : "-"}`,
  "",
  "This issue is generated automatically and does not include logs or secrets.",
].join("\n");

async function request(path, options = {}) {
  const response = await fetch(`${apiBase}${path}`, {
    ...options,
    headers: {
      Accept: "application/vnd.github+json",
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      "X-GitHub-Api-Version": "2022-11-28",
      ...(options.headers || {}),
    },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`GitHub API ${response.status}: ${text}`);
  }

  if (response.status === 204) return null;
  return response.json();
}

async function main() {
  const query = encodeURIComponent(`repo:${ownerRepo} is:issue is:open in:title "${title}"`);
  const search = await request(`/search/issues?q=${query}`);
  const existing = (search.items || [])[0];

  if (existing) {
    await request(`/repos/${owner}/${repo}/issues/${existing.number}/comments`, {
      method: "POST",
      body: JSON.stringify({ body }),
    });
    console.log(`Updated failure notification issue #${existing.number}.`);
    return;
  }

  const issue = await request(`/repos/${owner}/${repo}/issues`, {
    method: "POST",
    body: JSON.stringify({
      title,
      body,
      labels: ["actions-failure"],
    }),
  });
  console.log(`Created failure notification issue #${issue.number}.`);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
