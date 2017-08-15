package software.wings.delegatetasks;

import software.wings.service.impl.analysis.LogElement;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;

/**
 * Created by rsingh on 5/18/17.
 */
public interface LogAnalysisStoreService {
  void save(StateType stateType, String accountId, String appId, String stateExecutionId, String workflowId,
      String workflowExecutionId, String serviceId, List<LogElement> splunkLogs) throws IOException;
}
