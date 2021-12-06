package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.servicelevelobjective.beans.SLIMissingDataType;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.ThresholdSLIMetricSpec;
import io.harness.cvng.servicelevelobjective.services.api.SLIAnalyserService;

import com.google.common.base.Preconditions;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThresholdAnalyserServiceImpl implements SLIAnalyserService<ThresholdSLIMetricSpec> {
  @Override
  public SLIMissingDataType analyse(Map<String, Double> sliAnalyseRequest, ThresholdSLIMetricSpec sliSpec) {
    Double metricValue = sliAnalyseRequest.get(sliSpec.getMetric1());
    Preconditions.checkNotNull(
        metricValue, "metric value for metric identifier " + sliSpec.getMetric1() + " not found.");
    if (sliSpec.getThresholdType().compute(metricValue, sliSpec.getThresholdValue())) {
      return SLIMissingDataType.GOOD;
    } else {
      return SLIMissingDataType.BAD;
    }
  }
}
