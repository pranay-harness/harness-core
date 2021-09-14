/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.ServiceGuardTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.TimeSeriesRecordDTO;
import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.statemachine.beans.AnalysisInput;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface TrendAnalysisService {
  Map<String, ExecutionStatus> getTaskStatus(List<String> taskIds);

  String scheduleTrendAnalysisTask(AnalysisInput input);

  List<TimeSeriesRecordDTO> getTestData(String verificationTaskId, Instant startTime, Instant endTime);

  void saveAnalysis(String taskId, ServiceGuardTimeSeriesAnalysisDTO analysis);

  List<TimeSeriesMetricDefinition> getTimeSeriesMetricDefinitions();
}
