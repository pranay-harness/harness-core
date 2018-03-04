package software.wings.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.common.base.MoreObjects;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.ExecutionStatus;
import software.wings.yaml.BaseYaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Created by rishi on 10/30/16.
 */
public class NotificationRule {
  private String uuid = generateUuid();
  private List<ExecutionStatus> conditions = new ArrayList<>();
  private ExecutionScope executionScope;

  @NotNull @Size(min = 1) private List<NotificationGroup> notificationGroups = new ArrayList<>();

  private boolean batchNotifications;

  private boolean active = true;

  /**
   * Gets notification groups.
   *
   * @return the notification groups
   */
  public List<NotificationGroup> getNotificationGroups() {
    return notificationGroups;
  }

  /**
   * Sets notification groups.
   *
   * @param notificationGroups the notification groups
   */
  public void setNotificationGroups(List<NotificationGroup> notificationGroups) {
    this.notificationGroups = notificationGroups;
  }

  /**
   * Is active boolean.
   *
   * @return the boolean
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Sets active.
   *
   * @param active the active
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * Gets conditions.
   *
   * @return the conditions
   */
  public List<ExecutionStatus> getConditions() {
    return conditions;
  }

  /**
   * Sets conditions.
   *
   * @param conditions the conditions
   */
  public void setConditions(List<ExecutionStatus> conditions) {
    this.conditions = conditions;
  }

  /**
   * Gets execution scope.
   *
   * @return the execution scope
   */
  public ExecutionScope getExecutionScope() {
    return executionScope;
  }

  /**
   * Sets execution scope.
   *
   * @param executionScope the execution scope
   */
  public void setExecutionScope(ExecutionScope executionScope) {
    this.executionScope = executionScope;
  }

  /**
   * Gets uuid.
   *
   * @return the uuid
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Sets uuid.
   *
   * @param uuid the uuid
   */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  /**
   * Is batch notifications boolean.
   *
   * @return the boolean
   */
  public boolean isBatchNotifications() {
    return batchNotifications;
  }

  /**
   * Sets batch notifications.
   *
   * @param batchNotifications the batch notifications
   */
  public void setBatchNotifications(boolean batchNotifications) {
    this.batchNotifications = batchNotifications;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uuid", uuid)
        .add("conditions", conditions)
        .add("executionScope", executionScope)
        .add("notificationGroups", notificationGroups)
        .add("batchNotifications", batchNotifications)
        .add("active", active)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, conditions, executionScope, notificationGroups, batchNotifications, active);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final NotificationRule other = (NotificationRule) obj;
    return Objects.equals(this.uuid, other.uuid) && Objects.equals(this.conditions, other.conditions)
        && Objects.equals(this.executionScope, other.executionScope)
        && Objects.equals(this.notificationGroups, other.notificationGroups)
        && Objects.equals(this.batchNotifications, other.batchNotifications)
        && Objects.equals(this.active, other.active);
  }

  /**
   * The type Notification rule builder.
   */
  public static final class NotificationRuleBuilder {
    private String uuid = generateUuid();
    private List<ExecutionStatus> conditions = new ArrayList<>();
    private ExecutionScope executionScope;
    private List<NotificationGroup> notificationGroups = new ArrayList<>();
    private boolean batchNotifications;
    private boolean active = true;

    private NotificationRuleBuilder() {}

    /**
     * Add notification group notification rule builder.
     *
     * @param notificationGroup the notification group
     * @return the notification rule builder
     */
    public NotificationRuleBuilder addNotificationGroup(NotificationGroup notificationGroup) {
      this.notificationGroups.add(notificationGroup);
      return this;
    }

    /**
     * A notification rule notification rule builder.
     *
     * @return the notification rule builder
     */
    public static NotificationRuleBuilder aNotificationRule() {
      return new NotificationRuleBuilder();
    }

    /**
     * With uuid notification rule builder.
     *
     * @param uuid the uuid
     * @return the notification rule builder
     */
    public NotificationRuleBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With conditions notification rule builder.
     *
     * @param conditions the conditions
     * @return the notification rule builder
     */
    public NotificationRuleBuilder withConditions(List<ExecutionStatus> conditions) {
      this.conditions = conditions;
      return this;
    }

    /**
     * With execution scope notification rule builder.
     *
     * @param executionScope the execution scope
     * @return the notification rule builder
     */
    public NotificationRuleBuilder withExecutionScope(ExecutionScope executionScope) {
      this.executionScope = executionScope;
      return this;
    }

    /**
     * With notification groups notification rule builder.
     *
     * @param notificationGroups the notification groups
     * @return the notification rule builder
     */
    public NotificationRuleBuilder withNotificationGroups(List<NotificationGroup> notificationGroups) {
      this.notificationGroups = notificationGroups;
      return this;
    }

    /**
     * With batch notifications notification rule builder.
     *
     * @param batchNotifications the batch notifications
     * @return the notification rule builder
     */
    public NotificationRuleBuilder withBatchNotifications(boolean batchNotifications) {
      this.batchNotifications = batchNotifications;
      return this;
    }

    /**
     * With active notification rule builder.
     *
     * @param active the active
     * @return the notification rule builder
     */
    public NotificationRuleBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But notification rule builder.
     *
     * @return the notification rule builder
     */
    public NotificationRuleBuilder but() {
      return aNotificationRule()
          .withUuid(uuid)
          .withConditions(conditions)
          .withExecutionScope(executionScope)
          .withNotificationGroups(notificationGroups)
          .withBatchNotifications(batchNotifications)
          .withActive(active);
    }

    /**
     * Build notification rule.
     *
     * @return the notification rule
     */
    public NotificationRule build() {
      NotificationRule notificationRule = new NotificationRule();
      notificationRule.setUuid(uuid);
      notificationRule.setConditions(conditions);
      notificationRule.setExecutionScope(executionScope);
      notificationRule.setNotificationGroups(notificationGroups);
      notificationRule.setBatchNotifications(batchNotifications);
      notificationRule.setActive(active);
      return notificationRule;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseYaml {
    private List<String> conditions = new ArrayList<>();
    private String executionScope;
    private List<String> notificationGroups = new ArrayList<>();

    @Builder
    public Yaml(List<String> conditions, String executionScope, List<String> notificationGroups) {
      this.conditions = conditions;
      this.executionScope = executionScope;
      this.notificationGroups = notificationGroups;
    }
  }
}
