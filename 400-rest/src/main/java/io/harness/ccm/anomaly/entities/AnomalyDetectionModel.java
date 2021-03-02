package io.harness.ccm.anomaly.entities;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._490_CE_COMMONS)
public enum AnomalyDetectionModel {
  STATISTICAL,
  FBPROPHET;
}
