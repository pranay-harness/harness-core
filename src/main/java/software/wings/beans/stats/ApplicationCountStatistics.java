package software.wings.beans.stats;

/**
 * Created by anubhaw on 8/15/16.
 */
public class ApplicationCountStatistics extends WingsStatistics {
  private Integer count;

  /**
   * Instantiates a new Application count statistics.
   *
   * @param count the count
   */
  public ApplicationCountStatistics(Integer count) {
    super(StatisticsType.APPLICATION_COUNT);
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
