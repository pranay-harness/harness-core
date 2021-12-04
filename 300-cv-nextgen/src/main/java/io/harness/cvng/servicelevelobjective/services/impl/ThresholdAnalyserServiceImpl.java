package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.servicelevelobjective.beans.SLIAnalyseRequest;
import io.harness.cvng.servicelevelobjective.beans.SLIAnalyseResponse;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.ThresholdSLIMetricSpec;
import io.harness.cvng.servicelevelobjective.services.api.SLIAnalyserService;

import java.util.List;
import java.util.Map;

public class ThresholdAnalyserServiceImpl implements SLIAnalyserService<ThresholdSLIMetricSpec> {
  @Override
  public List<SLIAnalyseResponse> analyse(
      Map<String, List<SLIAnalyseRequest>> sliAnalyseRequest, ThresholdSLIMetricSpec sliSpec) {
    return null;
  }
}
