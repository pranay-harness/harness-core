package software.wings.beans;

/**
 * Created by anubhaw on 7/27/16.
 */
public class InformationNotification extends Notification {
  private String displayText;

  /**
   * Instantiates a new Information notification.
   */
  public InformationNotification() {
    super(NotificationType.INFORMATION);
  }

  /**
   * Gets display text.
   *
   * @return the display text
   */
  public String getDisplayText() {
    return displayText;
  }

  /**
   * Sets display text.
   *
   * @param displayText the display text
   */
  public void setDisplayText(String displayText) {
    this.displayText = displayText;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String displayText;
    private String environmentId;
    private String entityId;
    private EntityType entityType;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * An information notification builder.
     *
     * @return the builder
     */
    public static Builder anInformationNotification() {
      return new Builder();
    }

    /**
     * With display text builder.
     *
     * @param displayText the display text
     * @return the builder
     */
    public Builder withDisplayText(String displayText) {
      this.displayText = displayText;
      return this;
    }

    /**
     * With environment id builder.
     *
     * @param environmentId the environment id
     * @return the builder
     */
    public Builder withEnvironmentId(String environmentId) {
      this.environmentId = environmentId;
      return this;
    }

    /**
     * With entity id builder.
     *
     * @param entityId the entity id
     * @return the builder
     */
    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    /**
     * With entity type builder.
     *
     * @param entityType the entity type
     * @return the builder
     */
    public Builder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anInformationNotification()
          .withDisplayText(displayText)
          .withEnvironmentId(environmentId)
          .withEntityId(entityId)
          .withEntityType(entityType)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build information notification.
     *
     * @return the information notification
     */
    public InformationNotification build() {
      InformationNotification informationNotification = new InformationNotification();
      informationNotification.setDisplayText(displayText);
      informationNotification.setEnvironmentId(environmentId);
      informationNotification.setEntityId(entityId);
      informationNotification.setEntityType(entityType);
      informationNotification.setUuid(uuid);
      informationNotification.setAppId(appId);
      informationNotification.setCreatedBy(createdBy);
      informationNotification.setCreatedAt(createdAt);
      informationNotification.setLastUpdatedBy(lastUpdatedBy);
      informationNotification.setLastUpdatedAt(lastUpdatedAt);
      informationNotification.setActive(active);
      return informationNotification;
    }
  }
}
