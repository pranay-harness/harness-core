/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.CollectionUtils;
import io.harness.yaml.BaseYaml;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
public class NotificationRule {
  private String uuid = generateUuid();
  private List<ExecutionStatus> conditions = new ArrayList<>();
  private ExecutionScope executionScope;

  @Getter @Setter private boolean notificationGroupAsExpression;
  @Getter @Setter private boolean userGroupAsExpression;

  @NotNull @Size(min = 1) @Deprecated private List<NotificationGroup> notificationGroups = new ArrayList<>();

  @NotNull @Setter private List<String> userGroupIds = new ArrayList<>();

  @NotNull @Setter private String userGroupExpression;

  private boolean batchNotifications;

  private boolean active = true;

  /**
   * Even if this is empty, we might still have user groups to notify based on {@link #getUserGroupExpressions()}
   * @return
   */
  public List<String> getUserGroupIds() {
    return CollectionUtils.emptyIfNull(userGroupIds);
  }

  /**
   * This should be used to get user groups to notify if {@link #userGroupAsExpression} is set.
   */
  public String getUserGroupExpression() {
    return userGroupExpression;
  }

  /**
   * Gets notification groups.
   *
   * @return the notification groups
   * @deprecated user {@link #getUserGroupIds()} instead
   */
  @Deprecated
  public List<NotificationGroup> getNotificationGroups() {
    return notificationGroups;
  }

  /**
   * Sets notification groups.
   *
   * @param notificationGroups the notification groups
   * @deprecated use {@link #setUserGroupIds(List)} instead
   */
  @Deprecated
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
        .add("userGroupIds", userGroupIds)
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
        && this.executionScope == other.executionScope
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
    private boolean notificationGroupAsExpression;
    private boolean userGroupAsExpression;
    private String userGroupExpression;
    private List<String> userGroupIds = new ArrayList<>();

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

    public NotificationRuleBuilder withUserGroupIds(List<String> userGroupIds) {
      this.userGroupIds = CollectionUtils.emptyIfNull(userGroupIds);
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

    public NotificationRuleBuilder withNotificationGroupAsExpression(boolean notificationGroupAsExpression) {
      this.notificationGroupAsExpression = notificationGroupAsExpression;
      return this;
    }

    public NotificationRuleBuilder withUserGroupAsExpression(boolean userGroupAsExpression) {
      this.userGroupAsExpression = userGroupAsExpression;
      return this;
    }

    public NotificationRuleBuilder withUserGroupExpression(String userGroupExpression) {
      this.userGroupExpression = userGroupExpression;
      return this;
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
      notificationRule.setUserGroupIds(userGroupIds);
      notificationRule.setUserGroupAsExpression(userGroupAsExpression);
      notificationRule.setUserGroupExpression(userGroupExpression);
      notificationRule.setNotificationGroupAsExpression(notificationGroupAsExpression);
      return notificationRule;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseYaml {
    private List<String> conditions = new ArrayList<>();
    private String executionScope;
    private List<String> notificationGroups = new ArrayList<>();
    private boolean notificationGroupAsExpression;
    private boolean userGroupAsExpression;
    private String userGroupExpression;

    private List<String> userGroupIds = new ArrayList<>();
    private List<String> userGroupNames = new ArrayList<>();

    @Builder
    public Yaml(List<String> conditions, String executionScope, List<String> notificationGroups,
        boolean notificationGroupAsExpression, boolean userGroupAsExpression, List<String> userGroupIds,
        String userGroupExpression, List<String> userGroupNames) {
      this.conditions = conditions;
      this.executionScope = executionScope;
      this.notificationGroups = notificationGroups;
      this.notificationGroupAsExpression = notificationGroupAsExpression;
      this.userGroupAsExpression = userGroupAsExpression;
      this.userGroupIds = userGroupIds;
      this.userGroupNames = userGroupNames;
      this.userGroupExpression = userGroupExpression;
    }
  }
}
