package io.harness.cvng.core.utils.monitoredService;

import io.harness.cvng.core.beans.monitoredService.MetricPackDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.AppDynamicsHealthSourceSpec;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.stream.Collectors;

public class AppDynamicsHealthSourceSpecTransformer
    implements CVConfigToHealthSourceTransformer<AppDynamicsCVConfig, AppDynamicsHealthSourceSpec> {
  @Override
  public AppDynamicsHealthSourceSpec transformToHealthSourceConfig(List<AppDynamicsCVConfig> cvConfigs) {
    Preconditions.checkArgument(cvConfigs.stream().map(AppDynamicsCVConfig::getApplicationName).distinct().count() == 1,
        "Application Name should be same for List of all configs.");
    Preconditions.checkArgument(
        cvConfigs.stream().map(AppDynamicsCVConfig::getConnectorIdentifier).distinct().count() == 1,
        "ConnectorRef should be same for List of all configs.");
    Preconditions.checkArgument(cvConfigs.stream().map(AppDynamicsCVConfig::getTierName).distinct().count() == 1,
        "Application tier name should be same for List of all configs.");
    Preconditions.checkArgument(cvConfigs.stream().map(AppDynamicsCVConfig::getProductName).distinct().count() == 1,
        "Application feature name should be same for List of all configs.");

    return AppDynamicsHealthSourceSpec.builder()
        .applicationName(cvConfigs.get(0).getApplicationName())
        .connectorRef(cvConfigs.get(0).getConnectorIdentifier())
        .tierName(cvConfigs.get(0).getTierName())
        .feature(cvConfigs.get(0).getProductName())
        .metricPacks(
            cvConfigs.stream().map(cv -> MetricPackDTO.toMetricPackDTO(cv.getMetricPack())).collect(Collectors.toSet()))
        .build();
  }
}
