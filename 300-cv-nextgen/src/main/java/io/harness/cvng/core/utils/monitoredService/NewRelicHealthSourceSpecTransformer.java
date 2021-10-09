package io.harness.cvng.core.utils.monitoredService;

import io.harness.cvng.core.beans.monitoredService.MetricPackDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.NewRelicHealthSourceSpec;
import io.harness.cvng.core.entities.NewRelicCVConfig;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.stream.Collectors;

public class NewRelicHealthSourceSpecTransformer
    implements CVConfigToHealthSourceTransformer<NewRelicCVConfig, NewRelicHealthSourceSpec> {
  @Override
  public NewRelicHealthSourceSpec transformToHealthSourceConfig(List<NewRelicCVConfig> cvConfigs) {
    Preconditions.checkArgument(cvConfigs.stream().map(NewRelicCVConfig::getApplicationName).distinct().count() == 1,
        "Application Name should be same for List of all configs.");
    Preconditions.checkArgument(
        cvConfigs.stream().map(NewRelicCVConfig::getConnectorIdentifier).distinct().count() == 1,
        "ConnectorRef should be same for List of all configs.");
    Preconditions.checkArgument(cvConfigs.stream().map(NewRelicCVConfig::getApplicationId).distinct().count() == 1,
        "Application Id should be same for List of all configs.");
    Preconditions.checkArgument(cvConfigs.stream().map(NewRelicCVConfig::getProductName).distinct().count() == 1,
        "Application feature name should be same for List of all configs.");
    return NewRelicHealthSourceSpec.builder()
        .applicationName(cvConfigs.get(0).getApplicationName())
        .connectorRef(cvConfigs.get(0).getConnectorIdentifier())
        .applicationId(String.valueOf(cvConfigs.get(0).getApplicationId()))
        .feature(cvConfigs.get(0).getProductName())
        .metricPacks(
            cvConfigs.stream().map(cv -> MetricPackDTO.toMetricPackDTO(cv.getMetricPack())).collect(Collectors.toSet()))
        .build();
  }
}
