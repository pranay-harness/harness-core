package software.wings.beans.stats;

/**
 * Created by anubhaw on 8/19/16.
 */
public class DayActivityStatistics {
  private int successCount;
  private int failureCount;
  private Long date;

  /**
   * Gets success count.
   *
   * @return the success count
   */
  public int getSuccessCount() {
    return successCount;
  }

  /**
   * Sets success count.
   *
   * @param successCount the success count
   */
  public void setSuccessCount(int successCount) {
    this.successCount = successCount;
  }

  /**
   * Gets failure count.
   *
   * @return the failure count
   */
  public int getFailureCount() {
    return failureCount;
  }

  /**
   * Sets failure count.
   *
   * @param failureCount the failure count
   */
  public void setFailureCount(int failureCount) {
    this.failureCount = failureCount;
  }

  /**
   * Gets day start epoch.
   *
   * @return the day start epoch
   */
  public Long getDate() {
    return date;
  }

  /**
   * Sets day start epoch.
   *
   * @param date the day start epoch
   */
  public void setDate(Long date) {
    this.date = date;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private int successCount;
    private int failureCount;
    private Long date;

    private Builder() {}

    /**
     * A day activity statistics builder.
     *
     * @return the builder
     */
    public static Builder aDayActivityStatistics() {
      return new Builder();
    }

    /**
     * With success count builder.
     *
     * @param successCount the success count
     * @return the builder
     */
    public Builder withSuccessCount(int successCount) {
      this.successCount = successCount;
      return this;
    }

    /**
     * With failure count builder.
     *
     * @param failureCount the failure count
     * @return the builder
     */
    public Builder withFailureCount(int failureCount) {
      this.failureCount = failureCount;
      return this;
    }

    /**
     * With date builder.
     *
     * @param date the date
     * @return the builder
     */
    public Builder withDate(Long date) {
      this.date = date;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aDayActivityStatistics().withSuccessCount(successCount).withFailureCount(failureCount).withDate(date);
    }

    /**
     * Build day activity statistics.
     *
     * @return the day activity statistics
     */
    public DayActivityStatistics build() {
      DayActivityStatistics dayActivityStatistics = new DayActivityStatistics();
      dayActivityStatistics.setSuccessCount(successCount);
      dayActivityStatistics.setFailureCount(failureCount);
      dayActivityStatistics.setDate(date);
      return dayActivityStatistics;
    }
  }
}
