package software.wings.beans.stats;

/**
 * Created by anubhaw on 10/19/16.
 */
public class NotificationCount extends WingsStatistics {
  private int pendingNotificationsCount;
  private int completedNotificationsCount;
  private int failureCount;

  /**
   * Instantiates a new Notification counter.
   */
  public NotificationCount() {
    super(StatisticsType.NOTIFICATION_COUNT);
  }

  /**
   * Gets pending notifications count.
   *
   * @return the pending notifications count
   */
  public int getPendingNotificationsCount() {
    return pendingNotificationsCount;
  }

  /**
   * Sets pending notifications count.
   *
   * @param pendingNotificationsCount the pending notifications count
   */
  public void setPendingNotificationsCount(int pendingNotificationsCount) {
    this.pendingNotificationsCount = pendingNotificationsCount;
  }

  /**
   * Gets completed notifications count.
   *
   * @return the completed notifications count
   */
  public int getCompletedNotificationsCount() {
    return completedNotificationsCount;
  }

  /**
   * Sets completed notifications count.
   *
   * @param completedNotificationsCount the completed notifications count
   */
  public void setCompletedNotificationsCount(int completedNotificationsCount) {
    this.completedNotificationsCount = completedNotificationsCount;
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
   * The type Builder.
   */
  public static final class Builder {
    private int pendingNotificationsCount;
    private int completedNotificationsCount;
    private int failureCount;

    private Builder() {}

    /**
     * A notification count builder.
     *
     * @return the builder
     */
    public static Builder aNotificationCount() {
      return new Builder();
    }

    /**
     * With pending notifications count builder.
     *
     * @param pendingNotificationsCount the pending notifications count
     * @return the builder
     */
    public Builder withPendingNotificationsCount(int pendingNotificationsCount) {
      this.pendingNotificationsCount = pendingNotificationsCount;
      return this;
    }

    /**
     * With completed notifications count builder.
     *
     * @param completedNotificationsCount the completed notifications count
     * @return the builder
     */
    public Builder withCompletedNotificationsCount(int completedNotificationsCount) {
      this.completedNotificationsCount = completedNotificationsCount;
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aNotificationCount()
          .withPendingNotificationsCount(pendingNotificationsCount)
          .withCompletedNotificationsCount(completedNotificationsCount)
          .withFailureCount(failureCount);
    }

    /**
     * Build notification count.
     *
     * @return the notification count
     */
    public NotificationCount build() {
      NotificationCount notificationCount = new NotificationCount();
      notificationCount.setPendingNotificationsCount(pendingNotificationsCount);
      notificationCount.setCompletedNotificationsCount(completedNotificationsCount);
      notificationCount.setFailureCount(failureCount);
      return notificationCount;
    }
  }
}
