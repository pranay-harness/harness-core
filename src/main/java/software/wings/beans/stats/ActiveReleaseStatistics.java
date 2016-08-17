package software.wings.beans.stats;

/**
 * Created by anubhaw on 8/15/16.
 */
public class ActiveReleaseStatistics extends WingsStatistics {
  private Integer count;

  /**
   * Instantiates a new Active release statistics.
   *
   * @param count the count
   */
  public ActiveReleaseStatistics(Integer count) {
    super(StatisticsType.ACTIVE_RELEASES);
    this.count = count;
  }

  /**
   * Gets count.
   *
   * @return the count
   */
  public Integer getCount() {
    return count;
  }

  /**
   * Sets count.
   *
   * @param count the count
   */
  public void setCount(Integer count) {
    this.count = count;
  }
}
