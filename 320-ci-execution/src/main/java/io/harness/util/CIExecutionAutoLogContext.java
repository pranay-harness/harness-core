package io.harness.util;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;

public class CIExecutionAutoLogContext extends AutoLogContext {
  private static final String PIPELINE_IDENTIFIER = "pipelineId";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  public CIExecutionAutoLogContext(String pipelineId, String projectIdentifier, String orgIdentifier,
      String accountIdentifier, OverrideBehavior behavior) {
    super(ImmutableMap.of(PIPELINE_IDENTIFIER, pipelineId, PROJECT_IDENTIFIER, projectIdentifier, ORG_IDENTIFIER,
              orgIdentifier, ACCOUNT_IDENTIFIER, accountIdentifier),
        behavior);
  }
}
