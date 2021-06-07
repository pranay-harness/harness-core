package io.harness.ng.core.delegate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateResourceValidationResponse {
  private List<Boolean> delegateValidityData;
}
