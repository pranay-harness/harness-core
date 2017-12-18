package software.wings.beans;

/**
 * Created by anubhaw on 7/28/16.
 */
public class NotificationAction {
  private String name;
  private NotificationActionType Type;
  private boolean primary;

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
   * Gets type.
   *
   * @return the type
   */
  public NotificationActionType getType() {
    return Type;
  }

  /**
   * Sets type.
   *
   * @param type the type
   */
  public void setType(NotificationActionType type) {
    Type = type;
  }

  /**
   * Is primary boolean.
   *
   * @return the boolean
   */
  public boolean isPrimary() {
    return primary;
  }

  /**
   * Sets primary.
   *
   * @param primary the primary
   */
  public void setPrimary(boolean primary) {
    this.primary = primary;
  }

  /**
   * The enum Notification action type.
   */
  public enum NotificationActionType {
    /**
     * Accept notification action type.
     */
    APPROVE,
    /**
     * Reject notification action type.
     */
    REJECT,
    /**
     * Resume notification action type.
     */
    RESUME
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private NotificationActionType Type;
    private boolean primary;

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
     * With type builder.
     *
     * @param Type the type
     * @return the builder
     */
    public Builder withType(NotificationActionType Type) {
      this.Type = Type;
      return this;
    }

    /**
     * With primary builder.
     *
     * @param primary the primary
     * @return the builder
     */
    public Builder withPrimary(boolean primary) {
      this.primary = primary;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aNotificationAction().withName(name).withType(Type).withPrimary(primary);
    }

    /**
     * Build notification action.
     *
     * @return the notification action
     */
    public NotificationAction build() {
      NotificationAction notificationAction = new NotificationAction();
      notificationAction.setName(name);
      notificationAction.setType(Type);
      notificationAction.setPrimary(primary);
      return notificationAction;
    }
  }
}
