package io.harness.cvng.core.entities;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.beans.HealthSourceQueryType;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.MetricResponseMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@JsonTypeName("DYNATRACE")
@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "DynatraceCVConfigKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DynatraceCVConfig extends MetricCVConfig {
  private String serviceName;
  private long serviceEntityId;
  private String groupName;
  private List<DynatraceMetricInfo> metricInfos;
  private HealthSourceQueryType queryType;

  @Override
  protected void validateParams() {}

  @Override
  public DataSourceType getType() {
    return DataSourceType.DYNATRACE;
  }

  @Override
  public String getDataCollectionDsl() {
    return getMetricPack().getDataCollectionDsl();
  }

  public static class DynatraceCVConfigUpdatableEntity
      extends MetricCVConfigUpdatableEntity<DynatraceCVConfig, DynatraceCVConfig> {
    @Override
    public void setUpdateOperations(
        UpdateOperations<DynatraceCVConfig> updateOperations, DynatraceCVConfig dynatraceCVConfig) {
      setCommonOperations(updateOperations, dynatraceCVConfig);
      updateOperations.set(DynatraceCVConfigKeys.serviceName, dynatraceCVConfig.getServiceName())
          .set(DynatraceCVConfigKeys.serviceEntityId, dynatraceCVConfig.getServiceEntityId());
    }
  }

  @Value
  @SuperBuilder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class DynatraceMetricInfo extends AnalysisInfo {
    String metricName;
    TimeSeriesMetricType metricType;
    MetricResponseMapping responseMapping;
  }
}
