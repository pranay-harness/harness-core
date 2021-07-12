package io.harness.cvng.core.beans;

import io.harness.cvng.beans.MonitoredServiceDataSourceType;
import io.harness.cvng.models.VerificationType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonitoredServiceDataSourceTypeDTO {
  MonitoredServiceDataSourceType dataSourceType;
  VerificationType verificationType;
}
