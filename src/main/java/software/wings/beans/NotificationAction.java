package software.wings.beans;

/**
 * Created by anubhaw on 7/22/16.
 */
public class NotificationAction {
  private String name;
  private String url;

  /**
   * Instantiates a new Notification action.
   */
  public NotificationAction() {}

  /**
   * Instantiates a new Notification action.
   *
   * @param name the name
   * @param url  the url
   */
  public NotificationAction(String name, String url) {
    this.name = name;
    this.url = url;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets url.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets url.
   *
   * @param url the url
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String url;

    private Builder() {}

    /**
     * A notification action builder.
     *
     * @return the builder
     */
    public static Builder aNotificationAction() {
      return new Builder();
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With url builder.
     *
     * @param url the url
     * @return the builder
     */
    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aNotificationAction().withName(name).withUrl(url);
    }

    /**
     * Build notification action.
     *
     * @return the notification action
     */
    public NotificationAction build() {
      NotificationAction notificationAction = new NotificationAction();
      notificationAction.setName(name);
      notificationAction.setUrl(url);
      return notificationAction;
    }
  }
}
