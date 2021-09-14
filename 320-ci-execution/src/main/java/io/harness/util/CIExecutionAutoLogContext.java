/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.util;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;

import com.google.common.collect.ImmutableMap;

@OwnedBy(HarnessTeam.CI)
public class CIExecutionAutoLogContext extends AutoLogContext {
  private static final String PIPELINE_IDENTIFIER = "pipelineId";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String EXECUTION_ID = "buildNumber";
  public CIExecutionAutoLogContext(String pipelineId, String projectIdentifier, String orgIdentifier,
      String accountIdentifier, String executionId, OverrideBehavior behavior) {
    super(ImmutableMap.of(PIPELINE_IDENTIFIER, pipelineId, PROJECT_IDENTIFIER, projectIdentifier, ORG_IDENTIFIER,
              orgIdentifier, ACCOUNT_IDENTIFIER, accountIdentifier, EXECUTION_ID, executionId),
        behavior);
  }
}
