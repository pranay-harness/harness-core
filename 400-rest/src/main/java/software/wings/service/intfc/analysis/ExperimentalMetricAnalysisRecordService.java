package software.wings.service.intfc.analysis;

import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;

public interface ExperimentalMetricAnalysisRecordService {
  ExperimentalMetricAnalysisRecord getLastAnalysisRecord(String stateExecutionId, String experimentName);

  ExperimentalMetricAnalysisRecord getAnalysisRecordForMinute(String stateExecutionId, Integer analysisMinute);
}
