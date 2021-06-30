package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.MetricPackService;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricPackDTO {
  @NotNull private CVMonitoringCategory identifier;

  public MetricPack toMetricPack(String accountId, String orgId, String projectId, DataSourceType dataSourceType,
      MetricPackService metricPackService) {
    return metricPackService.getMetricPack(accountId, orgId, projectId, dataSourceType, identifier);
  }

  public static MetricPackDTO toMetricPackDTO(MetricPack metricPack) {
    return MetricPackDTO.builder().identifier(metricPack.getCategory()).build();
  }
}
