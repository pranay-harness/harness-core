package io.harness.cvng.core.utils.monitoredService;

import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.DatadogLogHealthSourceSpec;
import io.harness.cvng.core.entities.DatadogLogCVConfig;

import java.util.List;

public class DatadogLogHealthSourceSpecTransformer
    implements CVConfigToHealthSourceTransformer<DatadogLogCVConfig, DatadogLogHealthSourceSpec> {
  @Override
  public DatadogLogHealthSourceSpec transformToHealthSourceConfig(List<DatadogLogCVConfig> cvConfigs) {
    return null;
  }
}
