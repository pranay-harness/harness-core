package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum AccountEventType {
  APP_CREATED,
  SERVICE_CREATED,
  ENV_CREATED,
  WORKFLOW_CREATED,
  WORKFLOW_DEPLOYED,
  PIPELINE_DEPLOYED,
  DELEGATE_INSTALLED,
  CLOUD_PROVIDER_CREATED,
  ARTIFACT_REPO_CREATED,
  PIPELINE_CREATED,
  ARTIFACT_STREAM_ADDED,
  INFRA_MAPPING_ADDED,
  INFRA_DEFINITION_ADDED,
  CUSTOM
}
