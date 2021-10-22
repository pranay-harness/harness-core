package io.harness.cvng.core.entities;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.beans.DatadogMetricHealthDefinition;
import io.harness.serializer.JsonUtils;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

@JsonTypeName("DATADOG")
@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "DatadogCVConfigKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DatadogMetricCVConfig extends MetricCVConfig {
    private List<MetricInfo> metricInfoList;
    private String dashboardName;

    public void fromMetricDefinitions(List<DatadogMetricHealthDefinition> datadogMetricDefinitions, CVMonitoringCategory category) {
        Preconditions.checkNotNull(datadogMetricDefinitions);
        if (metricInfoList == null) {
            metricInfoList = new ArrayList<>();
        }
        dashboardName = datadogMetricDefinitions.get(0).getDashboardName();
        MetricPack metricPack = MetricPack.builder()
                .category(category)
                .accountId(getAccountId())
                .dataSourceType(DataSourceType.DATADOG_METRICS)
                .projectIdentifier(getProjectIdentifier())
                .identifier(category.getDisplayName())
                .build();

        datadogMetricDefinitions.forEach(definition -> {
            TimeSeriesMetricType metricType = definition.getRiskProfile().getMetricType();
            metricInfoList.add(MetricInfo.builder()
                    .metricName(definition.getMetricName())
                    .query(definition.getQuery())
                    .metricType(metricType)
                    .tags(definition.getMetricTags())
                    .isManualQuery(definition.isManualQuery())
                    .serviceInstanceField(definition.getServiceInstanceField())
                    .build());

            // add this metric to the pack and the corresponding thresholds
            Set<TimeSeriesThreshold> thresholds = getThresholdsToCreateOnSaveForCustomProviders(
                    definition.getMetricName(), metricType, definition.getRiskProfile().getThresholdTypes());
            metricPack.addToMetrics(MetricPack.MetricDefinition.builder()
                    .thresholds(new ArrayList<>(thresholds))
                    .type(metricType)
                    .name(definition.getMetricName())
                    .included(true)
                    .build());
        });
        this.setMetricPack(metricPack);
    }

    @Data
    @Builder
    @FieldNameConstants(innerTypeName = "MetricInfoKeys")
    public static class MetricInfo {
        private String metricName;
        private String query;
        private List<String> tags;
        private TimeSeriesMetricType metricType;
        boolean isManualQuery;
        private String serviceInstanceField;
    }
    @Override
    protected void validateParams() {
      checkNotNull(metricInfoList, generateErrorMessageFromParam(DatadogMetricCVConfig.DatadogCVConfigKeys.metricInfoList));

    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.DATADOG_METRICS;
    }

    @Override
    public String getDataCollectionDsl() {
        return getMetricPack().getDataCollectionDsl();
    }

    public static class DatadogMetricCVConfigUpdatableEntity
            extends MetricCVConfigUpdatableEntity<DatadogMetricCVConfig, DatadogMetricCVConfig> {
        @Override
        public void setUpdateOperations(
                UpdateOperations<DatadogMetricCVConfig> updateOperations, DatadogMetricCVConfig stackdriverCVConfig) {
            setCommonOperations(updateOperations, stackdriverCVConfig);
            updateOperations.set(DatadogCVConfigKeys.metricInfoList, stackdriverCVConfig.getMetricInfoList());
            updateOperations.set(DatadogCVConfigKeys.dashboardName, stackdriverCVConfig.getDashboardName());
        }
    }
}
