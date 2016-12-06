package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
@Entity(value = "delegateTasks", noClassnameStored = true)
public class DelegateTask extends Base {
  private TaskType taskType;
  private Object[] parameters;
  private String tag;
  private String accountId;
  private String waitId;

  /**
   * Getter for property 'taskType'.
   *
   * @return Value for property 'taskType'.
   */
  public TaskType getTaskType() {
    return taskType;
  }

  /**
   * Setter for property 'taskType'.
   *
   * @param taskType Value to set for property 'taskType'.
   */
  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  /**
   * Getter for property 'parameters'.
   *
   * @return Value for property 'parameters'.
   */
  public Object[] getParameters() {
    return parameters;
  }

  /**
   * Setter for property 'parameters'.
   *
   * @param parameters Value to set for property 'parameters'.
   */
  public void setParameters(Object[] parameters) {
    this.parameters = parameters;
  }

  /**
   * Getter for property 'tag'.
   *
   * @return Value for property 'tag'.
   */
  public String getTag() {
    return tag;
  }

  /**
   * Setter for property 'tag'.
   *
   * @param tag Value to set for property 'tag'.
   */
  public void setTag(String tag) {
    this.tag = tag;
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
   * Getter for property 'waitId'.
   *
   * @return Value for property 'waitId'.
   */
  public String getWaitId() {
    return waitId;
  }

  /**
   * Setter for property 'waitId'.
   *
   * @param waitId Value to set for property 'waitId'.
   */
  public void setWaitId(String waitId) {
    this.waitId = waitId;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(taskType, parameters, tag, accountId, waitId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final DelegateTask other = (DelegateTask) obj;
    return Objects.equals(this.taskType, other.taskType) && Objects.deepEquals(this.parameters, other.parameters)
        && Objects.equals(this.tag, other.tag) && Objects.equals(this.accountId, other.accountId)
        && Objects.equals(this.waitId, other.waitId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("taskType", taskType)
        .add("parameters", parameters)
        .add("tag", tag)
        .add("accountId", accountId)
        .add("waitId", waitId)
        .toString();
  }

  public enum TaskType { HTTP, SPLUNK, APP_DYNAMICS }

  public static final class Builder {
    private TaskType taskType;
    private Object[] parameters;
    private String tag;
    private String accountId;
    private String waitId;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static Builder aDelegateTask() {
      return new Builder();
    }

    public Builder withTaskType(TaskType taskType) {
      this.taskType = taskType;
      return this;
    }

    public Builder withParameters(Object[] parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder withTag(String tag) {
      this.tag = tag;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withWaitId(String waitId) {
      this.waitId = waitId;
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
      return aDelegateTask()
          .withTaskType(taskType)
          .withParameters(parameters)
          .withTag(tag)
          .withAccountId(accountId)
          .withWaitId(waitId)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public DelegateTask build() {
      DelegateTask delegateTask = new DelegateTask();
      delegateTask.setTaskType(taskType);
      delegateTask.setParameters(parameters);
      delegateTask.setTag(tag);
      delegateTask.setAccountId(accountId);
      delegateTask.setWaitId(waitId);
      delegateTask.setUuid(uuid);
      delegateTask.setAppId(appId);
      delegateTask.setCreatedBy(createdBy);
      delegateTask.setCreatedAt(createdAt);
      delegateTask.setLastUpdatedBy(lastUpdatedBy);
      delegateTask.setLastUpdatedAt(lastUpdatedAt);
      return delegateTask;
    }
  }
}
