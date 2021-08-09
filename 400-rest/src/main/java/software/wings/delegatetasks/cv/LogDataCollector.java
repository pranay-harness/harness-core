package software.wings.delegatetasks.cv;

import software.wings.service.impl.analysis.LogDataCollectionInfoV2;
import software.wings.service.impl.analysis.LogElement;

import java.util.List;

public interface LogDataCollector<T extends LogDataCollectionInfoV2> extends DataCollector<T> {
  /**
   * Called with list of hosts. This needs to be thread safe.
   * @param hostBatch
   * @return Returns list of log elements.
   */
  List<LogElement> fetchLogs(List<String> hostBatch) throws DataCollectionException;

  /**
   * Fetch logs for all the host. This needs to be thread safe.
   * @return Returns list of log elements.
   */
  List<LogElement> fetchLogs() throws DataCollectionException;
}
