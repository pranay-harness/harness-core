package io.harness.yaml.snippets;

import io.harness.yaml.snippets.bean.YamlSnippetTags;

public enum SnippetTag implements YamlSnippetTags {
  k8s,
  git,
  docker,
  connector,
  secretmanager,
  secret,
  secretText,
  secretFile,
  sshKey,
  service,
  infra,
  steps,
  pipeline,
  http
}
