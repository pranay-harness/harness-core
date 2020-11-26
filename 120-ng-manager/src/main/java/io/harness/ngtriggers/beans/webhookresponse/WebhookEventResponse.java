package io.harness.ngtriggers.beans.webhookresponse;

import io.harness.ngtriggers.beans.target.pipeline.TargetExecutionSummary;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebhookEventResponse {
  public enum FinalStatus {
    SCM_SERVICE_CONNECTION_FAILED,
    INVALID_PAYLOAD,
    NO_MATCHING_TRIGGER_FOR_REPO,
    NO_MATCHING_TRIGGER_FOR_PAYLOAD_CONDITIONS,
    INVALID_RUNTIME_INPUT_YAML,
    TARGET_DID_NOT_EXECUTE,
    TARGET_EXECUTION_REQUESTED
  }

  private String accountId;
  private String eventCorrelationId;
  private String payload;
  private long createdAt;
  private FinalStatus finalStatus;
  private String message;
  private String planExecutionId;
  private boolean exceptionOccurred;
  private String triggerIdentifier;
  private TargetExecutionSummary targetExecutionSummary;
}
