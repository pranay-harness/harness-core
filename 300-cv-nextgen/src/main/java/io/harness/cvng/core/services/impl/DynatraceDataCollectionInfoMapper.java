package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.DynatraceDataCollectionInfo;
import io.harness.cvng.core.entities.DynatraceCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

import java.util.ArrayList;
import java.util.List;

public class DynatraceDataCollectionInfoMapper
    implements DataCollectionInfoMapper<DynatraceDataCollectionInfo, DynatraceCVConfig> {
  @Override
  public DynatraceDataCollectionInfo toDataCollectionInfo(DynatraceCVConfig cvConfig) {
    return null;
  }

  private DynatraceDataCollectionInfo.MetricCollectionInfo getMetricCollectionInfo(
      DynatraceCVConfig.DynatraceMetricInfo metricInfo) {
    return DynatraceDataCollectionInfo.MetricCollectionInfo.builder().build();
  }

  private DynatraceDataCollectionInfo getDataCollectionInfo(
      List<DynatraceDataCollectionInfo.MetricCollectionInfo> metricDefinitions, DynatraceCVConfig cvConfig) {
//    DynatraceDataCollectionInfo dataCollectionInfo =
//        DynatraceDataCollectionInfo.builder().metricDefinitions(metricDefinitions).build();
//    dataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
//    return dataCollectionInfo;
    return null;
  }
}
