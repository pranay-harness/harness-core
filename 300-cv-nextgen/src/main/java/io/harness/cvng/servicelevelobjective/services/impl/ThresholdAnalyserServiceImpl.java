package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.servicelevelobjective.beans.slimetricspec.ThresholdSLIMetricSpec;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState;
import io.harness.cvng.servicelevelobjective.services.api.SLIAnalyserService;

import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThresholdAnalyserServiceImpl implements SLIAnalyserService<ThresholdSLIMetricSpec> {
  @Override
  public SLIState analyse(Map<String, Double> sliAnalyseRequest, ThresholdSLIMetricSpec sliSpec) {
    Double metricValue = sliAnalyseRequest.get(sliSpec.getMetric1());
    if (Objects.isNull(metricValue)) {
      return SLIState.NO_DATA;
    }
    if (sliSpec.getThresholdType().compute(metricValue, sliSpec.getThresholdValue())) {
      return SLIState.GOOD;
    } else {
      return SLIState.BAD;
    }
  }
}
