package software.wings.beans;

/**
 * Created by anubhaw on 7/22/16.
 */
public class NotificationAction {
  private String name;
  private String url;

  public NotificationAction() {}

  public NotificationAction(String name, String url) {
    this.name = name;
    this.url = url;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public static final class Builder {
    private String name;
    private String url;

    private Builder() {}

    public static Builder aNotificationAction() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder but() {
      return aNotificationAction().withName(name).withUrl(url);
    }

    public NotificationAction build() {
      NotificationAction notificationAction = new NotificationAction();
      notificationAction.setName(name);
      notificationAction.setUrl(url);
      return notificationAction;
    }
  }
}
