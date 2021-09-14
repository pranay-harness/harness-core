/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.analysis;

import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;

public interface TimeSeriesMLAnalysisRecordService {
  TimeSeriesMLAnalysisRecord getLastAnalysisRecord(String stateExecutionId);

  TimeSeriesMLAnalysisRecord getAnalysisRecordForMinute(String stateExecutionId, Integer analysisMinute);
}
