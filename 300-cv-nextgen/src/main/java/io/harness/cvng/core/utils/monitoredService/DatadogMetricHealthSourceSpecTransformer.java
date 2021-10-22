package io.harness.cvng.core.utils.monitoredService;

import com.google.common.base.Preconditions;
import io.harness.cvng.core.beans.DatadogMetricHealthDefinition;
import io.harness.cvng.core.beans.RiskProfile;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.DatadogMetricHealthSourceSpec;
import io.harness.cvng.core.entities.DatadogMetricCVConfig;

import java.util.ArrayList;
import java.util.List;

public class DatadogMetricHealthSourceSpecTransformer implements CVConfigToHealthSourceTransformer<DatadogMetricCVConfig, DatadogMetricHealthSourceSpec> {

    @Override
    public DatadogMetricHealthSourceSpec transformToHealthSourceConfig(List<DatadogMetricCVConfig> cvConfigs) {
        Preconditions.checkNotNull(cvConfigs);
        Preconditions.checkArgument(
                cvConfigs.stream().map(DatadogMetricCVConfig::getConnectorIdentifier).distinct().count() == 1,
                "ConnectorRef should be same for all the configs in the list.");
        List<DatadogMetricHealthDefinition> metricDefinitions = new ArrayList<>();
        cvConfigs.forEach(cvConfig -> cvConfig.getMetricInfoList().forEach(metricInfo -> {
            DatadogMetricHealthDefinition metricDefinition =
                    DatadogMetricHealthDefinition.builder()
                            .metricTags(metricInfo.getTags())
                            .dashboardName(cvConfig.getDashboardName())
                            .metricName(metricInfo.getMetricName())
                            .serviceInstanceField(metricInfo.getServiceInstanceField())
                            .query(metricInfo.getQuery())
                            .isManualQuery(metricInfo.isManualQuery())

                            .riskProfile(
                                    RiskProfile.builder()
                                            .category(cvConfig.getMetricPack().getCategory())
                                            .metricType(metricInfo.getMetricType())
                                            .thresholdTypes(cvConfig.getThresholdTypeOfMetric(metricInfo.getMetricName(), cvConfig))
                                            .build())
                            .build();

            metricDefinitions.add(metricDefinition);
        }));
        return DatadogMetricHealthSourceSpec.builder()
                .metricDefinitions(metricDefinitions)
                .connectorRef(cvConfigs.get(0).getConnectorIdentifier())
                .build();
    }
}
