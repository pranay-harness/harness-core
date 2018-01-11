package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 7/22/16.
 */
@Entity(value = "notifications")
@JsonTypeInfo(use = Id.NAME, property = "notificationType", include = As.EXISTING_PROPERTY)
public abstract class Notification extends Base {
  private String environmentId;
  private String entityId;
  private EntityType entityType;
  private String accountId;

  @NotNull private NotificationType notificationType;
  @Indexed @NotNull private boolean complete = true;
  @Indexed @NotNull private boolean actionable;

  private String notificationTemplateId;
  private Map<String, String> notificationTemplateVariables = new HashMap<>();

  /**
   * Instantiates a new Notification.
   */
  public Notification() {}

  /**
   * Instantiates a new Notification.
   *
   * @param notificationType the notification type
   */
  public Notification(NotificationType notificationType) {
    this(notificationType, false);
  }

  /**
   * Instantiates a new Notification.
   *
   * @param notificationType the notification type
   * @param actionable       the actionable
   */
  public Notification(NotificationType notificationType, boolean actionable) {
    this.notificationType = notificationType;
    this.actionable = actionable;
    this.complete = !actionable; // actionable notification are not complete on creation
  }

  /**
   * Gets environment id.
   *
   * @return the environment id
   */
  public String getEnvironmentId() {
    return environmentId;
  }

  /**
   * Sets environment id.
   *
   * @param environmentId the environment id
   */
  public void setEnvironmentId(String environmentId) {
    this.environmentId = environmentId;
  }

  /**
   * Gets entity id.
   *
   * @return the entity id
   */
  public String getEntityId() {
    return entityId;
  }

  /**
   * Sets entity id.
   *
   * @param entityId the entity id
   */
  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  /**
   * Gets entity type.
   *
   * @return the entity type
   */
  public EntityType getEntityType() {
    return entityType;
  }

  /**
   * Sets entity type.
   *
   * @param entityType the entity type
   */
  public void setEntityType(EntityType entityType) {
    this.entityType = entityType;
  }

  /**
   * Gets notification type.
   *
   * @return the notification type
   */
  public NotificationType getNotificationType() {
    return notificationType;
  }

  /**
   * Is complete boolean.
   *
   * @return the boolean
   */
  public boolean isComplete() {
    return complete;
  }

  /**
   * Sets complete.
   *
   * @param complete the complete
   */
  public void setComplete(boolean complete) {
    this.complete = complete;
  }

  /**
   * Is actionable boolean.
   *
   * @return the boolean
   */
  public boolean isActionable() {
    return actionable;
  }

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * Gets notification template id.
   *
   * @return the notification template id
   */
  public String getNotificationTemplateId() {
    return notificationTemplateId;
  }

  /**
   * Sets notification template id.
   *
   * @param notificationTemplateId the notification template id
   */
  public void setNotificationTemplateId(String notificationTemplateId) {
    this.notificationTemplateId = notificationTemplateId;
  }

  public Map<String, String> getNotificationTemplateVariables() {
    return notificationTemplateVariables;
  }

  public void setNotificationTemplateVariables(Map<String, String> notificationTemplateVariables) {
    this.notificationTemplateVariables = notificationTemplateVariables;
  }

  /**
   * The enum Notification type.
   */
  public enum NotificationType {
    /**
     * Approval notification type.
     */
    APPROVAL,
    /**
     * Change notification type.
     */
    CHANGE,
    /**
     * Failure notification type.
     */
    FAILURE,
    /**
     * Information notification type.
     */
    INFORMATION
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("environmentId", environmentId)
        .add("entityId", entityId)
        .add("entityType", entityType)
        .add("accountId", accountId)
        .add("notificationType", notificationType)
        .add("complete", complete)
        .add("actionable", actionable)
        .toString();
  }
}
