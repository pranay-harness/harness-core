<#list tailPatterns as tailPattern>
if [[ ! -v filePatterns ]]; then
  filePatterns="${tailPattern.filePath}|${tailPattern.pattern}"
else
  filePatterns="$filePatterns|${tailPattern.filePath}|${tailPattern.pattern}"
fi
</#list>

if ! harness_utils_start_tail_log_verification "$filePatterns"; then
  exit 1
fi

${commandString}

harness_utils_wait_for_tail_log_verification
