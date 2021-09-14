/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
  pagerduty,
  argoconnector
}
