package software.wings.service.impl.analysis;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.HeatMap;
import software.wings.verification.TimeSeriesOfMetric;
import software.wings.verification.TransactionTimeSeries;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.text.ParseException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public interface ContinuousVerificationService {
  void saveCVExecutionMetaData(ContinuousVerificationExecutionMetaData continuousVerificationExecutionMetaData);
  LinkedHashMap<Long,
      LinkedHashMap<String,
          LinkedHashMap<String,
              LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
  getCVExecutionMetaData(String accountId, long beginEpochTs, long endEpochTs, User user) throws ParseException;

  List<CVDeploymentData> getCVDeploymentData(
      String accountId, long startTime, long endTime, User user, String serviceId);

  List<WorkflowExecution> getDeploymentsForService(
      String accountId, long startTime, long endTime, User user, String serviceId);

  void setMetaDataExecutionStatus(String stateExecutionId, ExecutionStatus status, boolean noData);
  PageResponse<ContinuousVerificationExecutionMetaData> getAllCVExecutionsForTime(String accountId, long beginEpochTs,
      long endEpochTs, boolean isTimeSeries, PageRequest<ContinuousVerificationExecutionMetaData> pageRequest);

  List<HeatMap> getHeatMap(
      String accountId, String appId, String serviceId, long startTime, long endTime, boolean detailed);

  SortedSet<TransactionTimeSeries> getTimeSeriesOfHeatMapUnit(TimeSeriesFilter filter);

  Map<String, Map<String, TimeSeriesOfMetric>> fetchObservedTimeSeries(
      long startTime, long endTime, CVConfiguration cvConfiguration, long historyStartTime);

  VerificationNodeDataSetupResponse getDataForNode(
      String accountId, String serverConfigId, Object fetchConfig, StateType type);

  boolean notifyVerificationState(String correlationId, VerificationDataAnalysisResponse response);

  boolean notifyWorkflowVerificationState(String appId, String stateExecutionId, ExecutionStatus status);

  boolean collect247Data(String cvConfigId, StateType stateType, long startTime, long endTime);

  boolean collectCVDataForWorkflow(String contextId, long collectionMinute);

  boolean openAlert(String cvConfigId, ContinuousVerificationAlertData alertData);

  List<ContinuousVerificationExecutionMetaData> getCVDeploymentData(
      PageRequest<ContinuousVerificationExecutionMetaData> pageRequest);

  boolean collectCVData(String cvTaskId);

  boolean createCVTask247(String cvTaskId, Instant startTime, Instant endTime);

  VerificationStateAnalysisExecutionData getVerificationStateExecutionData(String stateExecutionId);
}
