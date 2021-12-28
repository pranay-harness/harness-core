package io.harness.cvng.core.utils.monitoredService;

import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.DynatraceHealthSourceSpec;
import io.harness.cvng.core.entities.DynatraceCVConfig;

import java.util.List;

public class DynatraceHealthSourceSpecTransformer
    implements CVConfigToHealthSourceTransformer<DynatraceCVConfig, DynatraceHealthSourceSpec> {
  @Override
  public DynatraceHealthSourceSpec transformToHealthSourceConfig(List<DynatraceCVConfig> cvConfigs) {
    return null;
  }
}
