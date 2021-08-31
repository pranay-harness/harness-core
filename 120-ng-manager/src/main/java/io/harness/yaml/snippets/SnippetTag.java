package io.harness.yaml.snippets;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.snippets.bean.YamlSnippetTags;

@OwnedBy(DX)
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
  http,
  splunk,
  appdynamics,
  vault,
  azurekeyvault,
  local,
  gcpkms,
  gcp,
  aws,
  awskms,
  awssecretmanager,
  artifactory,
  jira,
  nexus,
  github,
  gitlab,
  bitbucket,
  ceaws,
  ceazure,
  cek8s,
  codecommit,
  httphelmrepo,
  newrelic,
  gcpcloudcost,
  prometheus,
  datadog,
  sumologic,
  dynatrace,
  pagerduty
}
