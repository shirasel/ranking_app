@echo off
setlocal

set "PROJECT_ROOT=%~dp0.."
pushd "%PROJECT_ROOT%" >nul

echo [1/6] Checking frontend JavaScript syntax...
node --check docs\assets\js\common.js || goto :failed
node --check docs\assets\js\core\domain\genre-catalog.js || goto :failed
node --check docs\assets\js\core\platform\url-service.js || goto :failed
node --check docs\assets\js\core\platform\dom-service.js || goto :failed
node --check docs\assets\js\core\presentation\formatter.js || goto :failed
node --check docs\assets\js\core\data\json-repository.js || goto :failed
node --check docs\assets\js\core\services\generation-health-service.js || goto :failed
node --check docs\assets\js\core\services\youtube-url-service.js || goto :failed
node --check docs\assets\js\core\presentation\rank-change-presenter.js || goto :failed
node --check docs\assets\js\core\presentation\genre-link-renderer.js || goto :failed
node --check docs\assets\js\core\presentation\ranking-card-renderer.js || goto :failed
node --check docs\assets\js\core\presentation\ranking-list-renderer.js || goto :failed
node --check docs\assets\js\core\platform\theme-controller.js || goto :failed
node --check docs\assets\js\core\application\yt-rank-application.js || goto :failed
node --check docs\assets\js\ranking.js || goto :failed
node --check docs\assets\js\video.js || goto :failed
node --check docs\assets\js\operation-log.js || goto :failed
node --check tools\actions\write-validation-action-summary.js || goto :failed
node --check tools\actions\notify-action-failure-issue.js || goto :failed
node --check tools\diagnostics\check-youtube-api-key.js || goto :failed
node --check tools\preview\serve-github-pages-preview.js || goto :failed

echo [2/6] Running Kotlin tests and build...
call tools\run-gradle-local.cmd test build || goto :failed

echo [3/6] Generating mock ranking data...
call tools\generate-mock-rankings.cmd || goto :failed

echo [4/6] Validating generated public JSON...
call tools\validate-generated-data.cmd || goto :failed

echo [5/6] Checking validation summary output...
node tools\actions\write-validation-action-summary.js || goto :failed

echo [6/6] Verification completed.
popd >nul
exit /b 0

:failed
echo Verification failed.
popd >nul
exit /b 1
