package software.wings.service.impl.analysis;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.User;
import software.wings.sm.ExecutionStatus;
import software.wings.verification.HeatMap;
import software.wings.verification.TimeSeriesDataPoint;

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

  void setMetaDataExecutionStatus(String stateExecutionId, ExecutionStatus status);
  PageResponse<ContinuousVerificationExecutionMetaData> getAllCVExecutionsForTime(String accountId, long beginEpochTs,
      long endEpochTs, boolean isTimeSeries, PageRequest<ContinuousVerificationExecutionMetaData> pageRequest);

  Map<String, List<HeatMap>> getHeatMap(
      String accountId, String serviceId, int resolution, long startTime, long endTime, boolean detailed);
  Map<String, Map<String, List<TimeSeriesDataPoint>>> getTimeSeriesOfHeatMapUnit(
      String accountId, String cvConfigId, long startTime, long endTime);
}
