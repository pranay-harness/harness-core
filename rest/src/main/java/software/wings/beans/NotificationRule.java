package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import software.wings.sm.ExecutionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Created by rishi on 10/30/16.
 */

@Entity(value = "notificationRules", noClassnameStored = true)
public class NotificationRule extends Base {
  private List<ExecutionStatus> conditions = new ArrayList<>();

  private ExecutionScope executionScope;

  @NotNull @Size(min = 1) private Map<String, Object> notificationFilters;

  @NotNull @Size(min = 1) private List<NotificationGroup> notificationGroups = new ArrayList<>();

  private int batchIntervalInSecs;

  private boolean active = true;

  public Map<String, Object> getNotificationFilters() {
    return notificationFilters;
  }

  public void setNotificationFilters(Map<String, Object> notificationFilters) {
    this.notificationFilters = notificationFilters;
  }

  public List<NotificationGroup> getNotificationGroups() {
    return notificationGroups;
  }

  public void setNotificationGroups(List<NotificationGroup> notificationGroups) {
    this.notificationGroups = notificationGroups;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public List<ExecutionStatus> getConditions() {
    return conditions;
  }

  public void setConditions(List<ExecutionStatus> conditions) {
    this.conditions = conditions;
  }

  public ExecutionScope getExecutionScope() {
    return executionScope;
  }

  public void setExecutionScope(ExecutionScope executionScope) {
    this.executionScope = executionScope;
  }

  public int getBatchIntervalInSecs() {
    return batchIntervalInSecs;
  }

  public void setBatchIntervalInSecs(int batchIntervalInSecs) {
    this.batchIntervalInSecs = batchIntervalInSecs;
  }

  public static final class NotificationRuleBuilder {
    private List<ExecutionStatus> conditions = new ArrayList<>();
    private ExecutionScope executionScope;
    private Map<String, Object> notificationFilters;
    private List<NotificationGroup> notificationGroups = new ArrayList<>();
    private int batchIntervalInSecs;
    private boolean active = true;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private NotificationRuleBuilder() {}

    public static NotificationRuleBuilder aNotificationRule() {
      return new NotificationRuleBuilder();
    }

    public NotificationRuleBuilder withConditions(List<ExecutionStatus> conditions) {
      this.conditions = conditions;
      return this;
    }

    public NotificationRuleBuilder withExecutionScope(ExecutionScope executionScope) {
      this.executionScope = executionScope;
      return this;
    }

    public NotificationRuleBuilder withNotificationFilters(Map<String, Object> notificationFilters) {
      this.notificationFilters = notificationFilters;
      return this;
    }

    public NotificationRuleBuilder addNotificationGroup(NotificationGroup notificationGroup) {
      this.notificationGroups.add(notificationGroup);
      return this;
    }

    public NotificationRuleBuilder withBatchIntervalInSecs(int batchIntervalInSecs) {
      this.batchIntervalInSecs = batchIntervalInSecs;
      return this;
    }

    public NotificationRuleBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public NotificationRuleBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public NotificationRuleBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public NotificationRuleBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public NotificationRuleBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public NotificationRuleBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public NotificationRuleBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public NotificationRule build() {
      NotificationRule notificationRule = new NotificationRule();
      notificationRule.setConditions(conditions);
      notificationRule.setExecutionScope(executionScope);
      notificationRule.setNotificationFilters(notificationFilters);
      notificationRule.setNotificationGroups(notificationGroups);
      notificationRule.setBatchIntervalInSecs(batchIntervalInSecs);
      notificationRule.setActive(active);
      notificationRule.setUuid(uuid);
      notificationRule.setAppId(appId);
      notificationRule.setCreatedBy(createdBy);
      notificationRule.setCreatedAt(createdAt);
      notificationRule.setLastUpdatedBy(lastUpdatedBy);
      notificationRule.setLastUpdatedAt(lastUpdatedAt);
      return notificationRule;
    }
  }
}
