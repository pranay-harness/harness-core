package io.harness.cvng.core.services.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cvng.beans.DataSourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonTypeName("APP_DYNAMICS")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppDynamicsCVConfig extends MetricCVConfig {
  private long tierId;
  private long applicationId;
  private String applicationName;
  private String tierName;
  @Override
  public DataSourceType getType() {
    return DataSourceType.APP_DYNAMICS;
  }

  @Override
  public Map<String, Object> getDslEnvVariables() {
    Map<String, Object> dslEnvVariables = new HashMap<>();
    dslEnvVariables.put("appId", getApplicationId());
    dslEnvVariables.put("tierId", getTierId());
    final List<String> metricPaths = getMetricPack()
                                         .getMetrics()
                                         .stream()
                                         .filter(metricDefinition -> metricDefinition.isIncluded())
                                         .map(metricDefinition -> metricDefinition.getPath())
                                         .collect(Collectors.toList());
    dslEnvVariables.put("metricsToCollect", metricPaths);
    return dslEnvVariables;
  }

  @Override
  @JsonIgnore
  public String getDataCollectionDsl() {
    return getMetricPack().getDataCollectionDsl();
  }
}
