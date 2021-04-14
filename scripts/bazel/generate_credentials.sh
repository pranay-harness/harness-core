if [ -z "${JFROG_USERNAME}" ];
then
  echo "data-collection credentials are not set. See https://harness.slack.com/archives/C8DHL8G8P/p1603265514093900"
  exit 1
fi

cat > bazel-credentials.bzl <<EOF
JFROG_USERNAME="${JFROG_USERNAME}"
JFROG_PASSWORD="${JFROG_PASSWORD}"
EOF

scripts/bazel/testDistribute.sh