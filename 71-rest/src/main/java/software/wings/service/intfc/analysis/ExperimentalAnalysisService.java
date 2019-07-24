package software.wings.service.intfc.analysis;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.service.impl.analysis.ExpAnalysisInfo;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.newrelic.ExperimentalMetricRecord;
import software.wings.sm.StateType;

import java.util.List;

public interface ExperimentalAnalysisService {
  PageResponse<ExpAnalysisInfo> getMetricExpAnalysisInfoList(PageRequest<ExperimentalMetricAnalysisRecord> pageRequest);

  List<ExpAnalysisInfo> getLogExpAnalysisInfoList();

  ExperimentalMetricRecord getExperimentalMetricAnalysisSummary(
      String stateExecutionId, StateType stateType, String expName);

  LogMLAnalysisSummary getExperimentalAnalysisSummary(
      String stateExecutionId, String appId, StateType stateType, String expName);
}
