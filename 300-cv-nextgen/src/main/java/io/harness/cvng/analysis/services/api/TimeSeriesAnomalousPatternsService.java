/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.ServiceGuardTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;

import java.util.List;
import java.util.Map;

public interface TimeSeriesAnomalousPatternsService {
  void saveAnomalousPatterns(ServiceGuardTimeSeriesAnalysisDTO analysis, String verificationTaskId);
  Map<String, Map<String, List<TimeSeriesAnomalies>>> getLongTermAnomalies(String verificationTaskId);
}
