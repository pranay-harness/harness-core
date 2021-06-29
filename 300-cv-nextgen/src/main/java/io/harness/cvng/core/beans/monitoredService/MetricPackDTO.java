package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.MetricPack.MetricDefinition;

import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
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
  @NotNull DataSourceType type;
  @NotNull private CVMonitoringCategory category;
  @NotNull @Valid Set<MetricDefinitionDTO> metrics;

  public MetricPack toMetricPack(String accountId, String orgId, String projectId, MetricPackDTO msMetricPack) {
    return MetricPack.builder()
        .accountId(accountId)
        .orgIdentifier(orgId)
        .projectIdentifier(projectId)
        .category(msMetricPack.getCategory())
        .dataSourceType(msMetricPack.getType())
        .metrics(msMetricPack.getMetrics()
                     .stream()
                     .map(msMetricDefinition -> msMetricDefinition.toMetricDefinition(msMetricDefinition))
                     .collect(Collectors.toSet()))
        .build();
  }

  public static MetricPackDTO toMetricPackDTO(MetricPack metricPack) {
    return MetricPackDTO.builder()
        .category(metricPack.getCategory())
        .type(metricPack.getDataSourceType())
        .metrics(metricPack.getMetrics()
                     .stream()
                     .map(metricDefinition -> MetricDefinitionDTO.toMetricDefinitionDTO(metricDefinition))
                     .collect(Collectors.toSet()))
        .build();
  }

  @Data
  @Builder
  public static class MetricDefinitionDTO {
    @NotNull private String name;
    @NotNull private TimeSeriesMetricType type;

    private MetricPack.MetricDefinition toMetricDefinition(MetricDefinitionDTO msMetricDefinition) {
      return MetricPack.MetricDefinition.builder()
          .name(msMetricDefinition.getName())
          .type(msMetricDefinition.getType())
          .build();
    }

    private static MetricDefinitionDTO toMetricDefinitionDTO(MetricDefinition metricDefinition) {
      return MetricDefinitionDTO.builder().name(metricDefinition.getName()).type(metricDefinition.getType()).build();
    }
  }
}
