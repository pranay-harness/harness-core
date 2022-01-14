package io.harness.cvng.core.beans.monitoredService;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CountServiceDTO {
  Integer allServicesCount;
  Integer servicesAtRiskCount;
}
