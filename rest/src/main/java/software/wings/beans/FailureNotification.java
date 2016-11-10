package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import software.wings.beans.NotificationAction.NotificationActionType;

import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 7/25/16.
 */
@JsonTypeName("FAILURE")
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

  @Override
  public boolean performAction(NotificationActionType actionType) {
    return true;
  }

  public static final class Builder {
    private String entityName;
    private String executionId;
    private String environmentId;
    private String entityId;
    private EntityType entityType;
    private String accountId;
    private boolean complete = true;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static Builder aFailureNotification() {
      return new Builder();
    }

    public Builder withEntityName(String entityName) {
      this.entityName = entityName;
      return this;
    }

    public Builder withExecutionId(String executionId) {
      this.executionId = executionId;
      return this;
    }

    public Builder withEnvironmentId(String environmentId) {
      this.environmentId = environmentId;
      return this;
    }

    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public Builder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withComplete(boolean complete) {
      this.complete = complete;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder but() {
      return aFailureNotification()
          .withEntityName(entityName)
          .withExecutionId(executionId)
          .withEnvironmentId(environmentId)
          .withEntityId(entityId)
          .withEntityType(entityType)
          .withAccountId(accountId)
          .withComplete(complete)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public FailureNotification build() {
      FailureNotification failureNotification = new FailureNotification();
      failureNotification.setEntityName(entityName);
      failureNotification.setExecutionId(executionId);
      failureNotification.setEnvironmentId(environmentId);
      failureNotification.setEntityId(entityId);
      failureNotification.setEntityType(entityType);
      failureNotification.setAccountId(accountId);
      failureNotification.setComplete(complete);
      failureNotification.setUuid(uuid);
      failureNotification.setAppId(appId);
      failureNotification.setCreatedBy(createdBy);
      failureNotification.setCreatedAt(createdAt);
      failureNotification.setLastUpdatedBy(lastUpdatedBy);
      failureNotification.setLastUpdatedAt(lastUpdatedAt);
      return failureNotification;
    }
  }
}
