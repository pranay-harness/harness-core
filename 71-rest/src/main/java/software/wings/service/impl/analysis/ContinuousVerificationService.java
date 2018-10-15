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

public interface ContinuousVerificationService {
  void saveCVExecutionMetaData(ContinuousVerificationExecutionMetaData continuousVerificationExecutionMetaData);
  LinkedHashMap<Long,
      LinkedHashMap<String,
          LinkedHashMap<String,
              LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
  getCVExecutionMetaData(String accountId, long beginEpochTs, long endEpochTs, User user) throws ParseException;

  List<CVDeploymentData> getCVDeploymentData(
      String accountId, long beginEpochTs, long endEpochTs, User user, String serviceId);

  void setMetaDataExecutionStatus(String stateExecutionId, ExecutionStatus status);
  PageResponse<ContinuousVerificationExecutionMetaData> getAllCVExecutionsForTime(String accountId, long beginEpochTs,
      long endEpochTs, boolean isTimeSeries, PageRequest<ContinuousVerificationExecutionMetaData> pageRequest);

  List<HeatMap> getHeatMap(String accountId, String serviceId, int resolution, long startTime, long endTime);
  List<TimeSeriesDataPoint> getObservedTimeSeries(
      String accountId, String serviceId, int resolution, long startTime, long endTime);
  List<TimeSeriesDataPoint> getPredictedTimeSeries(
      String accountId, String serviceId, int resolution, long startTime, long endTime);
}
