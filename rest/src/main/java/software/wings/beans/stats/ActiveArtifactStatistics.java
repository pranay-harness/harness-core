package software.wings.beans.stats;

/**
 * Created by anubhaw on 8/17/16.
 */
public class ActiveArtifactStatistics extends WingsStatistics {
  private Integer count;

  /**
   * Instantiates a new Active artifact statistics.
   *
   * @param count the count
   */
  public ActiveArtifactStatistics(Integer count) {
    super(StatisticsType.ACTIVE_ARTIFACTS);
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
