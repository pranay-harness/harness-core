/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
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
