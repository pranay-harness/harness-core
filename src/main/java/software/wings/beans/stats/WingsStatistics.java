package software.wings.beans.stats;

/**
 * Created by anubhaw on 8/15/16.
 */
public abstract class WingsStatistics {
  private StatisticsType type;

  public WingsStatistics(StatisticsType type) {
    this.type = type;
  }

  public StatisticsType getType() {
    return type;
  }

  public enum StatisticsType {
    DEPLOYMENT,
    ACTIVE_RELEASES,
    ACTIVE_ARTIFACTS,
    APPLICATION_COUNT,
    DEPLOYMENT_ACTIVITIES,
    TOP_CONSUMERS
  }
}
