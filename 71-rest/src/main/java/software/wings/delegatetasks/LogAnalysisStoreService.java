package software.wings.delegatetasks;

import software.wings.service.impl.analysis.LogElement;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;

/**
 * Created by rsingh on 5/18/17.
 */
public interface LogAnalysisStoreService {
  boolean save(StateType stateType, String accountId, String appId, String stateExecutionId, String workflowId,
      String workflowExecutionId, String serviceId, String delegateTaskId, List<LogElement> splunkLogs)
      throws IOException;
}
