package io.harness.cvng.servicelevelobjective.services.api;

import io.harness.cvng.servicelevelobjective.beans.SLIMissingDataType;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.SLIMetricSpec;

import java.util.Map;

public interface SLIAnalyserService<T extends SLIMetricSpec> {
  SLIMissingDataType analyse(Map<String, Double> sliAnalyseRequest, T sliSpec);
}
