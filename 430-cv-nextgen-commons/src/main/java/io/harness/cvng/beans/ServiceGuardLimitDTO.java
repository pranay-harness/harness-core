package io.harness.cvng.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceGuardLimitDTO {
  long serviceGuardLimit;
}
