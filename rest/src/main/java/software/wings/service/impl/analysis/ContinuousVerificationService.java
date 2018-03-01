package software.wings.service.impl.analysis;

import software.wings.sm.ExecutionStatus;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public interface ContinuousVerificationService {
  void saveCVExecutionMetaData(ContinuousVerificationExecutionMetaData continuousVerificationExecutionMetaData);
  Map<Long, TreeMap<String, Map<String, Map<String, Map<String, List<ContinuousVerificationExecutionMetaData>>>>>>
  getCVExecutionMetaData(String accountId, long beginEpochTs, long endEpochTs) throws ParseException;

  void setMetaDataExecutionStatus(String stateExecutionId, ExecutionStatus status);
}
