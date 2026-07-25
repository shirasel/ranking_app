class GitHubApiClient {
  constructor(dependencies) {
    this.fetch = dependencies.fetch;
    this.apiBase = dependencies.apiBase;
    this.token = dependencies.token;
  }

  async request(path, options = {}) {
    const response = await this.fetch(`${this.apiBase}${path}`, {
      ...options,
      headers: {
        Accept: "application/vnd.github+json",
        Authorization: `Bearer ${this.token}`,
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
}

class ActionsContext {
  constructor(env) {
    this.ownerRepo = env.GITHUB_REPOSITORY || "";
    this.token = env.GITHUB_TOKEN || "";
    this.workflow = env.GITHUB_WORKFLOW || "GitHub Actions";
    this.runId = env.GITHUB_RUN_ID || "";
    this.runAttempt = env.GITHUB_RUN_ATTEMPT || "1";
    this.refName = env.GITHUB_REF_NAME || "";
    this.sha = env.GITHUB_SHA || "";
    this.serverUrl = env.GITHUB_SERVER_URL || "https://github.com";
  }

  isAvailable() {
    return Boolean(this.ownerRepo && this.token);
  }

  owner() {
    return this.ownerRepo.split("/")[0];
  }

  repo() {
    return this.ownerRepo.split("/")[1];
  }

  title() {
    return `[Actions failure] ${this.workflow}`;
  }

  runUrl() {
    return `${this.serverUrl}/${this.ownerRepo}/actions/runs/${this.runId}`;
  }
}

class FailureIssueBody {
  create(context) {
    return [
      `Workflow failed: ${context.workflow}`,
      "",
      `Run: ${context.runUrl()}`,
      `Attempt: ${context.runAttempt}`,
      `Branch: ${context.refName || "-"}`,
      `Commit: ${context.sha ? context.sha.slice(0, 12) : "-"}`,
      "",
      "This issue is generated automatically and does not include logs or secrets.",
    ].join("\n");
  }
}

class FailureLabelService {
  constructor(dependencies) {
    this.client = dependencies.client;
    this.context = dependencies.context;
    this.output = dependencies.output;
    this.label = "actions-failure";
  }

  async labels() {
    try {
      await this.client.request(`/repos/${this.context.owner()}/${this.context.repo()}/labels/${encodeURIComponent(this.label)}`);
      return [this.label];
    } catch (error) {
      if (!error.message.startsWith("GitHub API 404:")) {
        this.output.log("Failure label lookup failed. Creating issue without labels.");
        return [];
      }
    }

    try {
      await this.client.request(`/repos/${this.context.owner()}/${this.context.repo()}/labels`, {
        method: "POST",
        body: JSON.stringify({
          name: this.label,
          color: "d73a4a",
          description: "Created automatically when a GitHub Actions workflow fails.",
        }),
      });
      return [this.label];
    } catch (error) {
      this.output.log("Failure label creation failed. Creating issue without labels.");
      return [];
    }
  }
}

class FailureIssueNotifier {
  constructor(dependencies) {
    this.client = dependencies.client;
    this.context = dependencies.context;
    this.bodyFactory = dependencies.bodyFactory;
    this.labelService = dependencies.labelService;
    this.output = dependencies.output;
  }

  async run() {
    if (!this.context.isAvailable()) {
      this.output.log("GitHub failure issue notification skipped outside GitHub Actions.");
      return;
    }

    const body = this.bodyFactory.create(this.context);
    const existing = await this.findExistingIssue();
    if (existing) {
      await this.client.request(`/repos/${this.context.owner()}/${this.context.repo()}/issues/${existing.number}/comments`, {
        method: "POST",
        body: JSON.stringify({ body }),
      });
      this.output.log(`Updated failure notification issue #${existing.number}.`);
      return;
    }

    const labels = await this.labelService.labels();
    const issue = await this.client.request(`/repos/${this.context.owner()}/${this.context.repo()}/issues`, {
      method: "POST",
      body: JSON.stringify({
        title: this.context.title(),
        body,
        labels,
      }),
    });
    this.output.log(`Created failure notification issue #${issue.number}.`);
  }

  async findExistingIssue() {
    const query = encodeURIComponent(`repo:${this.context.ownerRepo} is:issue is:open in:title "${this.context.title()}"`);
    const search = await this.client.request(`/search/issues?q=${query}`);
    return (search.items || [])[0];
  }
}

const context = new ActionsContext(process.env);
const client = new GitHubApiClient({
  fetch,
  apiBase: "https://api.github.com",
  token: context.token,
});
const labelService = new FailureLabelService({
  client,
  context,
  output: console,
});

new FailureIssueNotifier({
  client,
  context,
  bodyFactory: new FailureIssueBody(),
  labelService,
  output: console,
}).run().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
