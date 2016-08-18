package software.wings.beans.stats;

import java.util.List;

/**
 * Created by anubhaw on 8/15/16.
 */
public class TopConsumersStatistics extends WingsStatistics {
  private List<TopConsumer> topConsumers;

  /**
   * Instantiates a new Top consumers statistics.
   *
   * @param topConsumers the top consumers
   */
  public TopConsumersStatistics(List<TopConsumer> topConsumers) {
    super(StatisticsType.TOP_CONSUMERS);
    this.topConsumers = topConsumers;
  }

  /**
   * Gets top consumers.
   *
   * @return the top consumers
   */
  public List<TopConsumer> getTopConsumers() {
    return topConsumers;
  }

  /**
   * Sets top consumers.
   *
   * @param topConsumers the top consumers
   */
  public void setTopConsumers(List<TopConsumer> topConsumers) {
    this.topConsumers = topConsumers;
  }
}
