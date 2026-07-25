const fs = require("fs");
const path = require("path");

const reportPath = path.join(process.cwd(), "docs", "data", "latest", "validation-report.json");
const summaryPath = process.env.GITHUB_STEP_SUMMARY;

function safeLine(value) {
  return String(value || "-").replace(/[\r\n|]/g, " ");
}

function writeSummary(markdown) {
  if (summaryPath) {
    fs.appendFileSync(summaryPath, markdown, "utf8");
  } else {
    process.stdout.write(markdown);
  }
}

if (!fs.existsSync(reportPath)) {
  writeSummary("## Generated JSON Validation\n\nValidation report was not generated.\n");
  process.exit(0);
}

const report = JSON.parse(fs.readFileSync(reportPath, "utf8"));
const status = report.status === "passed" ? "Passed" : "Failed";
const markdown = [
  "## Generated JSON Validation",
  "",
  "| Field | Value |",
  "| --- | --- |",
  `| Status | ${safeLine(status)} |`,
  `| Data generated at | ${safeLine(report.generatedAt)} |`,
  `| Errors | ${Number(report.errorCount || 0)} |`,
  `| Warnings | ${Number(report.warningCount || 0)} |`,
  "",
].join("\n");

writeSummary(markdown);
