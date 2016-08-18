package software.wings.beans.stats;

/**
 * Created by anubhaw on 8/17/16.
 */
public class ActiveArtifactStatistics extends WingsStatistics {
  private Integer count;

  public ActiveArtifactStatistics(Integer count) {
    super(StatisticsType.ACTIVE_ARTIFACTS);
    this.count = count;
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }
}
