package io.harness.delegate.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._940_DELEGATE_BEANS)
public enum TaskGroup {
  // *** NOTE: If you add an item to this list then also add an entry in catalogs.yml in the TASK_TYPES section. ***
  SCRIPT,
  HTTP,
  SPLUNK,
  APPDYNAMICS,
  INSTANA,
  NEWRELIC,
  STACKDRIVER,
  DYNA_TRACE,
  PROMETHEUS,
  CLOUD_WATCH,
  JENKINS,
  COMMAND,
  BAMBOO,
  DOCKER,
  ECR,
  GCR,
  GCS,
  GCB,
  GCP,
  ACR,
  NEXUS,
  S3,
  AZURE_ARTIFACTS,
  AZURE_VMSS,
  AZURE_APP_SERVICE,
  AZURE_ARM,
  ELK,
  LOGZ,
  SUMO,
  ARTIFACTORY,
  HOST_VALIDATION,
  KMS,
  GIT,
  CONTAINER,
  AMI,
  HELM,
  COLLABORATION_PROVIDER,
  PCF,
  SPOTINST,
  APM,
  LOG,
  CLOUD_FORMATION,
  TERRAFORM,
  AWS,
  LDAP,
  K8S,
  SMB,
  SFTP,
  TRIGGER,
  JIRA,
  CONNECTIVITY_VALIDATION,
  BUILD_SOURCE,
  CUSTOM,
  SHELL_SCRIPT_PROVISION,
  SERVICENOW,
  HELM_REPO_CONFIG_VALIDATION,
  HELM_VALUES_FETCH_TASK,
  GUARD_24x7,
  CI,
  SLACK,
  ARTIFACT_COLLECT_NG,
  K8S_NG,
  CAPABILITY_VALIDATION,
  JIRA_NG,
  CVNG,
  NOTIFICATION,
  HTTP_NG,
  SHELL_SCRIPT_NG,
  GIT_NG,
  BATCH_CAPABILITY_CHECK
}
