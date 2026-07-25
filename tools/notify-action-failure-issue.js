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
const failureLabel = "actions-failure";
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

async function ensureFailureLabel() {
  try {
    await request(`/repos/${owner}/${repo}/labels/${encodeURIComponent(failureLabel)}`);
    return [failureLabel];
  } catch (error) {
    if (!error.message.startsWith("GitHub API 404:")) {
      console.log("Failure label lookup failed. Creating issue without labels.");
      return [];
    }
  }

  try {
    await request(`/repos/${owner}/${repo}/labels`, {
      method: "POST",
      body: JSON.stringify({
        name: failureLabel,
        color: "d73a4a",
        description: "Created automatically when a GitHub Actions workflow fails.",
      }),
    });
    return [failureLabel];
  } catch (error) {
    console.log("Failure label creation failed. Creating issue without labels.");
    return [];
  }
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

  const labels = await ensureFailureLabel();
  const issue = await request(`/repos/${owner}/${repo}/issues`, {
    method: "POST",
    body: JSON.stringify({
      title,
      body,
      labels,
    }),
  });
  console.log(`Created failure notification issue #${issue.number}.`);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
