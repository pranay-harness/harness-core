package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.servicelevelobjective.beans.slimetricspec.RatioSLIMetricSpec;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState;
import io.harness.cvng.servicelevelobjective.services.api.SLIAnalyserService;

import com.google.common.base.Preconditions;
import java.util.Map;

public class RatioAnalyserServiceImpl implements SLIAnalyserService<RatioSLIMetricSpec> {
  @Override
  public SLIState analyse(Map<String, Double> sliAnalyseRequest, RatioSLIMetricSpec sliSpec) {
    Double metricValue1 = sliAnalyseRequest.get(sliSpec.getMetric1());
    Preconditions.checkNotNull(
        metricValue1, "metric value for metric identifier " + sliSpec.getMetric1() + " not found.");
    Double metricValue2 = sliAnalyseRequest.get(sliSpec.getMetric2());
    Preconditions.checkNotNull(
        metricValue2, "metric value for metric identifier " + sliSpec.getMetric2() + " not found.");
    if (metricValue2 == 0) {
      return SLIState.NO_DATA;
    }
    double metricValue = (metricValue1 / metricValue2) * 100;
    if (sliSpec.getThresholdType().compute(metricValue, sliSpec.getThresholdValue())) {
      return SLIState.GOOD;
    } else {
      return SLIState.BAD;
    }
  }
}
