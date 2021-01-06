package io.harness.ngtriggers.beans.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebhookEventResponse {
  public enum FinalStatus {
    SCM_SERVICE_CONNECTION_FAILED,
    INVALID_PAYLOAD,
    NO_MATCHING_TRIGGER_FOR_REPO,
    NO_MATCHING_TRIGGER_FOR_CONDITIONS,
    INVALID_RUNTIME_INPUT_YAML,
    TARGET_DID_NOT_EXECUTE,
    TARGET_EXECUTION_REQUESTED,
    NO_ENABLED_TRIGGER_FOUND_FOR_REPO
  }

  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private String targetIdentifier;
  private String eventCorrelationId;
  private String payload;
  private long createdAt;
  private FinalStatus finalStatus;
  private String message;
  private String planExecutionId;
  private boolean exceptionOccurred;
  private String triggerIdentifier;
  private TargetExecutionSummary targetExecutionSummary;
  private boolean enabled;
}
