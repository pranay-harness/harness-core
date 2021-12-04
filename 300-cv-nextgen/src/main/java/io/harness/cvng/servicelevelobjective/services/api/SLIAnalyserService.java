package io.harness.cvng.servicelevelobjective.services.api;

import io.harness.cvng.servicelevelobjective.beans.SLIAnalyseRequest;
import io.harness.cvng.servicelevelobjective.beans.SLIAnalyseResponse;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.SLIMetricSpec;

import java.util.List;
import java.util.Map;

public interface SLIAnalyserService<T extends SLIMetricSpec> {
  List<SLIAnalyseResponse> analyse(Map<String, List<SLIAnalyseRequest>> sliAnalyseRequest, T sliSpec);
}
