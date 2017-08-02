package software.wings.delegatetasks;

import software.wings.service.impl.analysis.LogElement;

import java.io.IOException;
import java.util.List;

/**
 * Created by rsingh on 5/18/17.
 */
public interface SplunkMetricStoreService {
  void save(String accountId, String appId, String stateExecutionId, String workflowExecutionId,
      List<LogElement> splunkLogs) throws IOException;
}
