/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.analysis;

import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;

public interface ExperimentalMetricAnalysisRecordService {
  ExperimentalMetricAnalysisRecord getLastAnalysisRecord(String stateExecutionId, String experimentName);

  ExperimentalMetricAnalysisRecord getAnalysisRecordForMinute(String stateExecutionId, Integer analysisMinute);
}
