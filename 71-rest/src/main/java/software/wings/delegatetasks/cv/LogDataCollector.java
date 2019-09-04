package software.wings.delegatetasks.cv;

import software.wings.service.impl.analysis.LogDataCollectionInfoV2;
import software.wings.service.impl.analysis.LogElement;

import java.util.List;
import java.util.Optional;

public interface LogDataCollector<T extends LogDataCollectionInfoV2> extends DataCollector<T> {
  /**
   * Called for each host or with empty host. This needs to be thread safe.
   * @param host
   * @return Returns list of log elements.
   */
  List<LogElement> fetchLogs(Optional<String> host) throws DataCollectionException;
}
