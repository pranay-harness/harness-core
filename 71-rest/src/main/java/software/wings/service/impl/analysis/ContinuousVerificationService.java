package software.wings.service.impl.analysis;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.sm.ExecutionStatus;
import software.wings.verification.CVConfiguration;
import software.wings.verification.HeatMap;
import software.wings.verification.TimeSeriesOfMetric;
import software.wings.verification.TransactionTimeSeries;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

  void setMetaDataExecutionStatus(String stateExecutionId, ExecutionStatus status);
  PageResponse<ContinuousVerificationExecutionMetaData> getAllCVExecutionsForTime(String accountId, long beginEpochTs,
      long endEpochTs, boolean isTimeSeries, PageRequest<ContinuousVerificationExecutionMetaData> pageRequest);

  List<HeatMap> getHeatMap(
      String accountId, String appId, String serviceId, long startTime, long endTime, boolean detailed);
  List<TransactionTimeSeries> getTimeSeriesOfHeatMapUnit(
      String accountId, String cvConfigId, long startTime, long endTime);
  Map<String, Map<String, TimeSeriesOfMetric>> fetchObservedTimeSeries(
      long startTime, long endTime, CVConfiguration cvConfiguration);
}
