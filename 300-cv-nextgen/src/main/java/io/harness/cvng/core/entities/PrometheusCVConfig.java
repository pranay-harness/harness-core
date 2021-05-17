package io.harness.cvng.core.entities;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.beans.PrometheusDSConfig.PrometheusFilter;
import io.harness.cvng.core.beans.PrometheusDSConfig.PrometheusMetricDefinition;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
public class PrometheusCVConfig extends MetricCVConfig {
  private String groupName;
  private List<MetricInfo> metricInfoList;

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "MetricInfoKeys")
  public static class MetricInfo {
    private String metricName;
    private String query;
    private String prometheusMetricName;
    private PrometheusFilter serviceFilter;
    private PrometheusFilter envFilter;
    private List<PrometheusFilter> additionalFilters;
    private String aggregation;
    private List<String> tags;
    private TimeSeriesMetricType metricType;
    boolean isManualQuery;
    private String serviceInstanceFieldName;
  }

  public void fromDSConfigDefinitions(
      List<PrometheusMetricDefinition> metricDefinitions, CVMonitoringCategory category) {
    if (metricInfoList == null) {
      metricInfoList = new ArrayList<>();
    }
    Preconditions.checkNotNull(metricDefinitions);
    MetricPack metricPack = MetricPack.builder()
                                .category(category)
                                .accountId(getAccountId())
                                .dataSourceType(DataSourceType.PROMETHEUS)
                                .projectIdentifier(getProjectIdentifier())
                                .identifier(category.getDisplayName())
                                .build();

    metricDefinitions.forEach(prometheusMetricDefinition -> {
      TimeSeriesMetricType metricType = prometheusMetricDefinition.getRiskProfile().getMetricType();
      metricInfoList.add(MetricInfo.builder()
                             .metricName(prometheusMetricDefinition.getMetricName())
                             .serviceFilter(prometheusMetricDefinition.getServiceFilter())
                             .envFilter(prometheusMetricDefinition.getEnvFilter())
                             .query(prometheusMetricDefinition.getQuery())
                             .isManualQuery(prometheusMetricDefinition.isManualQuery())
                             .metricType(metricType)
                             .additionalFilters(prometheusMetricDefinition.getAdditionalFilters())
                             .serviceInstanceFieldName(prometheusMetricDefinition.getServiceInstanceFieldName())
                             .prometheusMetricName(prometheusMetricDefinition.getPrometheusMetric())
                             .aggregation(prometheusMetricDefinition.getAggregation())
                             .build());

      // add the relevant thresholds to metricPack
      Set<TimeSeriesThreshold> thresholds =
          getThresholdsToCreateOnSaveForCustomProviders(prometheusMetricDefinition.getMetricName(), metricType,
              prometheusMetricDefinition.getRiskProfile().getThresholdTypes());
      metricPack.addToMetrics(MetricPack.MetricDefinition.builder()
                                  .thresholds(new ArrayList<>(thresholds))
                                  .type(metricType)
                                  .name(prometheusMetricDefinition.getMetricName())
                                  .included(true)
                                  .build());
    });
  }

  @Override
  protected void validateParams() {}

  @Override
  public DataSourceType getType() {
    return DataSourceType.PROMETHEUS;
  }

  @Override
  public String getDataCollectionDsl() {
    return getMetricPack().getDataCollectionDsl();
  }
}
