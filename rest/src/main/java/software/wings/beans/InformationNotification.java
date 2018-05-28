package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by anubhaw on 7/27/16.
 */
@JsonTypeName("INFORMATION")
@EqualsAndHashCode(callSuper = true)
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
    private String accountId;
    private boolean complete = true;
    private String notificationTemplateId;
    private Map<String, String> notificationTemplateVariables = new HashMap<>();
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static Builder anInformationNotification() {
      return new Builder();
    }

    public Builder withDisplayText(String displayText) {
      this.displayText = displayText;
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

    public Builder withNotificationTemplateId(String notificationTemplateId) {
      this.notificationTemplateId = notificationTemplateId;
      return this;
    }

    public Builder withNotificationTemplateVariables(Map<String, String> notificationTemplateVariables) {
      this.notificationTemplateVariables = notificationTemplateVariables;
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
      return anInformationNotification()
          .withDisplayText(displayText)
          .withEnvironmentId(environmentId)
          .withEntityId(entityId)
          .withEntityType(entityType)
          .withAccountId(accountId)
          .withComplete(complete)
          .withNotificationTemplateId(notificationTemplateId)
          .withNotificationTemplateVariables(notificationTemplateVariables)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public InformationNotification build() {
      InformationNotification informationNotification = new InformationNotification();
      informationNotification.setDisplayText(displayText);
      informationNotification.setEnvironmentId(environmentId);
      informationNotification.setEntityId(entityId);
      informationNotification.setEntityType(entityType);
      informationNotification.setAccountId(accountId);
      informationNotification.setComplete(complete);
      informationNotification.setNotificationTemplateId(notificationTemplateId);
      informationNotification.setNotificationTemplateVariables(notificationTemplateVariables);
      informationNotification.setUuid(uuid);
      informationNotification.setAppId(appId);
      informationNotification.setCreatedBy(createdBy);
      informationNotification.setCreatedAt(createdAt);
      informationNotification.setLastUpdatedBy(lastUpdatedBy);
      informationNotification.setLastUpdatedAt(lastUpdatedAt);
      return informationNotification;
    }
  }
}
