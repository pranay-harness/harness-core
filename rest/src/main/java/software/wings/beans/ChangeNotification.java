package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Created by anubhaw on 7/25/16.
 */
@JsonTypeName("CHANGE")
public class ChangeNotification extends Notification {
  private long scheduledOn;

  /**
   * Instantiates a new Change notification.
   */
  public ChangeNotification() {
    super(NotificationType.CHANGE);
  }

  /**
   * Gets scheduled on.
   *
   * @return the scheduled on
   */
  public long getScheduledOn() {
    return scheduledOn;
  }

  /**
   * Sets scheduled on.
   *
   * @param scheduledOn the scheduled on
   */
  public void setScheduledOn(long scheduledOn) {
    this.scheduledOn = scheduledOn;
  }

  public static final class Builder {
    private long scheduledOn;
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

    public static Builder aChangeNotification() {
      return new Builder();
    }

    public Builder withScheduledOn(long scheduledOn) {
      this.scheduledOn = scheduledOn;
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
      return aChangeNotification()
          .withScheduledOn(scheduledOn)
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

    public ChangeNotification build() {
      ChangeNotification changeNotification = new ChangeNotification();
      changeNotification.setScheduledOn(scheduledOn);
      changeNotification.setEnvironmentId(environmentId);
      changeNotification.setEntityId(entityId);
      changeNotification.setEntityType(entityType);
      changeNotification.setAccountId(accountId);
      changeNotification.setComplete(complete);
      changeNotification.setUuid(uuid);
      changeNotification.setAppId(appId);
      changeNotification.setCreatedBy(createdBy);
      changeNotification.setCreatedAt(createdAt);
      changeNotification.setLastUpdatedBy(lastUpdatedBy);
      changeNotification.setLastUpdatedAt(lastUpdatedAt);
      return changeNotification;
    }
  }
}
