package software.wings.beans;

import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 7/25/16.
 */
public class FailureNotification extends ActionableNotification {
  @NotNull private String entityName;
  @NotNull private String executionId;

  /**
   * Instantiates a new Failure notification.
   */
  public FailureNotification() {
    super(NotificationType.FAILURE);
  }

  /**
   * Gets entity name.
   *
   * @return the entity name
   */
  public String getEntityName() {
    return entityName;
  }

  /**
   * Sets entity name.
   *
   * @param entityName the entity name
   */
  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  /**
   * Gets execution id.
   *
   * @return the execution id
   */
  public String getExecutionId() {
    return executionId;
  }

  /**
   * Sets execution id.
   *
   * @param executionId the execution id
   */
  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String entityName;
    private String executionId;
    private String environmentId;
    private String entityId;
    private NotificationEntityType entityType;
    private boolean complete = true;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A failure notification builder.
     *
     * @return the builder
     */
    public static Builder aFailureNotification() {
      return new Builder();
    }

    /**
     * With entity name builder.
     *
     * @param entityName the entity name
     * @return the builder
     */
    public Builder withEntityName(String entityName) {
      this.entityName = entityName;
      return this;
    }

    /**
     * With execution id builder.
     *
     * @param executionId the execution id
     * @return the builder
     */
    public Builder withExecutionId(String executionId) {
      this.executionId = executionId;
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
    public Builder withEntityType(NotificationEntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    /**
     * With complete builder.
     *
     * @param complete the complete
     * @return the builder
     */
    public Builder withComplete(boolean complete) {
      this.complete = complete;
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
      return aFailureNotification()
          .withEntityName(entityName)
          .withExecutionId(executionId)
          .withEnvironmentId(environmentId)
          .withEntityId(entityId)
          .withEntityType(entityType)
          .withComplete(complete)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build failure notification.
     *
     * @return the failure notification
     */
    public FailureNotification build() {
      FailureNotification failureNotification = new FailureNotification();
      failureNotification.setEntityName(entityName);
      failureNotification.setExecutionId(executionId);
      failureNotification.setEnvironmentId(environmentId);
      failureNotification.setEntityId(entityId);
      failureNotification.setEntityType(entityType);
      failureNotification.setComplete(complete);
      failureNotification.setUuid(uuid);
      failureNotification.setAppId(appId);
      failureNotification.setCreatedBy(createdBy);
      failureNotification.setCreatedAt(createdAt);
      failureNotification.setLastUpdatedBy(lastUpdatedBy);
      failureNotification.setLastUpdatedAt(lastUpdatedAt);
      failureNotification.setActive(active);
      return failureNotification;
    }
  }
}
