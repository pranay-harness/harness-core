package software.wings.beans.stats;

import java.util.List;

/**
 * Created by anubhaw on 8/15/16.
 */
public class TopConsumersStatistics extends WingsStatistics {
  private List<TopConsumer> topConsumers;

  public TopConsumersStatistics(List<TopConsumer> topConsumers) {
    super(StatisticsType.TOP_CONSUMERS);
    this.topConsumers = topConsumers;
  }

  public List<TopConsumer> getTopConsumers() {
    return topConsumers;
  }

  public void setTopConsumers(List<TopConsumer> topConsumers) {
    this.topConsumers = topConsumers;
  }
}
