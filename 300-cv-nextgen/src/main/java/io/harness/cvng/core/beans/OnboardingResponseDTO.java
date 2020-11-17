package io.harness.cvng.core.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnboardingResponseDTO {
  private String accountId;
  private Object result;
  private String projectIdentifier;
  private String orgIdentifier;
  private String connectorIdentifier;
  private String tracingId;
}
