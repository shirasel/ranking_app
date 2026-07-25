const fs = require("fs");
const path = require("path");

class ValidationReportReader {
  constructor(fileSystem, reportPath) {
    this.fs = fileSystem;
    this.reportPath = reportPath;
  }

  exists() {
    return this.fs.existsSync(this.reportPath);
  }

  read() {
    return JSON.parse(this.fs.readFileSync(this.reportPath, "utf8"));
  }
}

class ActionSummaryWriter {
  constructor(fileSystem, output, summaryPath) {
    this.fs = fileSystem;
    this.output = output;
    this.summaryPath = summaryPath;
  }

  write(markdown) {
    if (this.summaryPath) {
      this.fs.appendFileSync(this.summaryPath, markdown, "utf8");
      return;
    }
    this.output.write(markdown);
  }
}

class ValidationSummaryMarkdown {
  safeLine(value) {
    return String(value || "-").replace(/[\r\n|]/g, " ");
  }

  create(report) {
    const status = report.status === "passed" ? "Passed" : "Failed";
    return [
      "## Generated JSON Validation",
      "",
      "| Field | Value |",
      "| --- | --- |",
      `| Status | ${this.safeLine(status)} |`,
      `| Data generated at | ${this.safeLine(report.generatedAt)} |`,
      `| Errors | ${Number(report.errorCount || 0)} |`,
      `| Warnings | ${Number(report.warningCount || 0)} |`,
      "",
    ].join("\n");
  }

  missingReport() {
    return "## Generated JSON Validation\n\nValidation report was not generated.\n";
  }
}

class ValidationSummaryCommand {
  constructor(dependencies) {
    this.reader = dependencies.reader;
    this.writer = dependencies.writer;
    this.markdown = dependencies.markdown;
  }

  run() {
    if (!this.reader.exists()) {
      this.writer.write(this.markdown.missingReport());
      return;
    }
    this.writer.write(this.markdown.create(this.reader.read()));
  }
}

new ValidationSummaryCommand({
  reader: new ValidationReportReader(
    fs,
    path.join(process.cwd(), "docs", "data", "latest", "validation-report.json")
  ),
  writer: new ActionSummaryWriter(fs, process.stdout, process.env.GITHUB_STEP_SUMMARY),
  markdown: new ValidationSummaryMarkdown(),
}).run();
