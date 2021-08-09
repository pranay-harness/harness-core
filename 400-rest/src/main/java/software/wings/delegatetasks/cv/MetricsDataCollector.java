package software.wings.delegatetasks.cv;

import software.wings.service.impl.analysis.MetricElement;
import software.wings.service.impl.analysis.MetricsDataCollectionInfo;

import java.util.List;

public interface MetricsDataCollector<T extends MetricsDataCollectionInfo> extends DataCollector<T> {
  /**
   * Called with list of hosts. This needs to be thread safe.
   * @param hostBatch
   * @return Returns list of metric element.
   */
  List<MetricElement> fetchMetrics(List<String> hostBatch) throws DataCollectionException;
  /**
   * Fetch metrics for all the host. This needs to be thread safe.
   * @return Returns list of log elements.
   */
  List<MetricElement> fetchMetrics() throws DataCollectionException;
}
