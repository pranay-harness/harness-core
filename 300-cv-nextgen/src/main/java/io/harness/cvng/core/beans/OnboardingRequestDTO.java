package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.cvng.beans.DataCollectionRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnboardingRequestDTO {
  private String connectorIdentifier;
  private String tracingId;
  private String accountId;
  private String projectIdentifier;
  private String orgIdentifier;
  private DataCollectionRequest dataCollectionRequest;
}
