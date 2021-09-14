/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.common;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public enum NGTaskType {
  DOCKER_ARTIFACT_TASK_NG,
  GCR_ARTIFACT_TASK_NG,
  ECR_ARTIFACT_TASK_NG,
  GIT_FETCH_NEXT_GEN_TASK,
  K8S_COMMAND_TASK_NG,
  K8S_COMMAND_TASK,
  JIRA_TASK_NG,
  HTTP_TASK_NG,
  GCP_TASK
}
