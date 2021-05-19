package io.harness.ngtriggers.beans.response;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PIPELINE)
public class WebhookEventResponse {
  public enum FinalStatus {
    SCM_SERVICE_CONNECTION_FAILED,
    INVALID_PAYLOAD,
    NO_MATCHING_TRIGGER_FOR_REPO,
    NO_MATCHING_TRIGGER_FOR_EVENT_ACTION,
    NO_MATCHING_TRIGGER_FOR_PAYLOAD_CONDITIONS,
    NO_MATCHING_TRIGGER_FOR_JEXL_CONDITIONS,
    NO_MATCHING_TRIGGER_FOR_HEADER_CONDITIONS,
    INVALID_RUNTIME_INPUT_YAML,
    TARGET_DID_NOT_EXECUTE,
    TARGET_EXECUTION_REQUESTED,
    NO_ENABLED_CUSTOM_TRIGGER_FOUND_FOR_PROJECT,
    NO_ENABLED_CUSTOM_TRIGGER_FOUND_FOR_ACCOUNT,
    NO_ENABLED_TRIGGER_FOR_PROJECT,
    NO_ENABLED_TRIGGER_FOR_ACCOUNT,
    NO_ENABLED_TRIGGER_FOR_SOURCEREPO_TYPE,
    NO_ENABLED_TRIGGER_FOR_ACCOUNT_SOURCE_REPO,
    FAILED_TO_FETCH_PR_DETAILS,
    EXCEPTION_WHILE_PROCESSING,
    TRIGGER_CONFIRMATION_SUCCESSFUL,
    TRIGGER_CONFIRMATION_FAILED
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
