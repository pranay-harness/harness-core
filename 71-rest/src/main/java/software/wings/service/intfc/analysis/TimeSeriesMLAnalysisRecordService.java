package software.wings.service.intfc.analysis;

import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;

public interface TimeSeriesMLAnalysisRecordService {
  TimeSeriesMLAnalysisRecord getLastAnalysisRecord(String stateExecutionId);

  TimeSeriesMLAnalysisRecord getAnalysisRecordForMinute(String stateExecutionId, Integer analysisMinute);
}
