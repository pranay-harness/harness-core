package io.harness.cvng.servicelevelobjective.services.api;

import io.harness.cvng.servicelevelobjective.beans.slimetricspec.SLIMetricSpec;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState;

import java.util.Map;

public interface SLIAnalyserService<T extends SLIMetricSpec> {
  SLIState analyse(Map<String, Double> sliAnalyseRequest, T sliSpec);
}
