package io.harness.cvng.core.utils.monitoredService;

import com.google.common.base.Preconditions;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.DatadogLogHealthSourceSpec;
import io.harness.cvng.core.entities.DatadogLogCVConfig;

import java.util.List;
import java.util.stream.Collectors;

public class DatadogLogHealthSourceSpecTransformer
    implements CVConfigToHealthSourceTransformer<DatadogLogCVConfig, DatadogLogHealthSourceSpec> {
  @Override
  public DatadogLogHealthSourceSpec transformToHealthSourceConfig(List<DatadogLogCVConfig> cvConfigs) {
    Preconditions.checkArgument(
            cvConfigs.stream().map(DatadogLogCVConfig::getConnectorIdentifier).distinct().count() == 1,
            "ConnectorRef should be same for List of all configs.");
    Preconditions.checkArgument(cvConfigs.stream().map(DatadogLogCVConfig::getProductName).distinct().count() == 1,
            "Application feature name should be same for List of all configs.");

    return DatadogLogHealthSourceSpec.builder()
            .connectorRef(cvConfigs.get(0).getConnectorIdentifier())
            .feature(cvConfigs.get(0).getProductName())
            .queries(cvConfigs.stream()
                    .map(cv
                            -> DatadogLogHealthSourceSpec.QueryDTO
                            .builder()
                            .name(cv.getQueryName())
                            .query(cv.getQuery())
                            .indexes(cv.getIndexes())
                            .serviceInstanceIdentifier(cv.getServiceInstanceIdentifier())
                            .build())
                    .collect(Collectors.toList()))
            .build();
  }
}
