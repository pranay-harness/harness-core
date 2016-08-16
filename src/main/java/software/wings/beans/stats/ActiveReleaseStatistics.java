package software.wings.beans.stats;

/**
 * Created by anubhaw on 8/15/16.
 */
public class ActiveReleaseStatistics extends WingsStatistics {
  private Integer count;

  public ActiveReleaseStatistics(Integer count) {
    super(StatisticsType.ACTIVE_RELEASES);
    this.count = count;
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }
}
