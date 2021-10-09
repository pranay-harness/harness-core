if [ -z "${JFROG_USERNAME}" ];
then
  echo "data-collection credentials are not set. See https://harness.slack.com/archives/C8DHL8G8P/p1603265514093900"
  exit 1
fi

#Setting up remote cache related config BT-282
if [ -z "${GOOGLE_CREDENTIALS_FILE}" ]
then
  GOOGLE_CREDENTIALS_FILE="platform-bazel-cache-dev.json"
  REMOTE_CACHE="https://storage.googleapis.com/harness-bazel-cache-us-dev"
  if date +"%Z" | grep -q 'IST'; then
    echo "Setting remote-cache in asia region"
    REMOTE_CACHE="https://storage.googleapis.com/harness-bazel-cache-blr-dev"
  fi
fi

echo build --google_credentials=${GOOGLE_CREDENTIALS_FILE} > bazelrc.gcp

cat <<EOT > bazelrc.cache
#Remote cache configuration
#build --remote_cache=${REMOTE_CACHE}
#build --remote_upload_local_results=false
#build --incompatible_remote_results_ignore_disk=true
build --experimental_guard_against_concurrent_changes
EOT

if [[ ! -f "$GOOGLE_CREDENTIALS_FILE" ]]; then
    curl -u "${JFROG_USERNAME}":"${JFROG_PASSWORD}" \
      https://harness.jfrog.io/artifactory/harness-internal/bazel/cache/platform-bazel-cache-dev.json \
      -o $GOOGLE_CREDENTIALS_FILE
fi

scripts/bazel/testDistribute.sh
