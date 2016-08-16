package software.wings.beans.stats;

import java.util.Map;

/**
 * Created by anubhaw on 8/15/16.
 */
public class TopConsumersStatistics extends WingsStatistics {
  Map<String, Integer> topConsumers;

  public TopConsumersStatistics(Map<String, Integer> topConsumers) {
    super(StatisticsType.TOP_CONSUMERS);
    this.topConsumers = topConsumers;
  }

  public Map<String, Integer> getTopConsumers() {
    return topConsumers;
  }

  public void setTopConsumers(Map<String, Integer> topConsumers) {
    this.topConsumers = topConsumers;
  }
}
