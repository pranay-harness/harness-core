package software.wings.beans.stats;

/**
 * Created by anubhaw on 8/15/16.
 */
public class ApplicationCountStatistics extends WingsStatistics {
  private Integer count;

  public ApplicationCountStatistics(Integer count) {
    super(StatisticsType.APPLICATION_COUNT);
    this.count = count;
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }
}
