package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.HealthSourceMetricDefinition;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.MetricPackService;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynatraceHealthSourceSpec extends HealthSourceSpec {
  @Override
  public String getConnectorRef() {
    return connectorRef;
  }

  @Override
  public DataSourceType getType() {
    return DataSourceType.DYNATRACE;
  }

  @Override
  public HealthSource.CVConfigUpdateResult getCVConfigUpdateResult(String accountId, String orgIdentifier,
      String projectIdentifier, String environmentRef, String serviceRef, String identifier, String name,
      List<CVConfig> existingCVConfigs, MetricPackService metricPackService) {
    return null;
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class DynatraceMetricDefinition extends HealthSourceMetricDefinition {
    String groupName;
  }

  @Value
  @Builder
  private static class Key {
    String serviceName;
    long serviceId;
    String envIdentifier;
    String serviceIdentifier;
    MetricPack metricPack;
  }
}
