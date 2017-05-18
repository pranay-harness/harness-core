package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.AlsoLoad;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import software.wings.beans.DelegateTask.Converter;
import software.wings.delegatetasks.DelegateRunnableTask;
import software.wings.utils.KryoUtils;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
@Entity(value = "delegateTasks", noClassnameStored = true)
@Converters(Converter.class)
public class DelegateTask extends Base {
  public static final int SYNC_CALL_TIMEOUT_INTERVAL = 25000;

  private TaskType taskType;
  private Object[] parameters;
  private String tag;
  private String accountId;
  private String waitId;
  @AlsoLoad("topicName") private String queueName;
  private Status status = Status.QUEUED;
  private String delegateId;
  @Transient private transient DelegateRunnableTask delegateRunnableTask;

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

  /**
   * Getter for property 'queueName'.
   *
   * @return Value for property 'queueName'.
   */
  public String getQueueName() {
    return queueName;
  }

  /**
   * Setter for property 'queueName'.
   *
   * @param queueName Value to set for property 'queueName'.
   */
  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  /**
   * Getter for property 'status'.
   *
   * @return Value for property 'status'.
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Setter for property 'status'.
   *
   * @param status Value to set for property 'status'.
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * Getter for property 'delegateId'.
   *
   * @return Value for property 'delegateId'.
   */
  public String getDelegateId() {
    return delegateId;
  }

  /**
   * Setter for property 'delegateId'.
   *
   * @param delegateId Value to set for property 'delegateId'.
   */
  public void setDelegateId(String delegateId) {
    this.delegateId = delegateId;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(taskType, parameters, tag, accountId, waitId, queueName, status, delegateId);
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
        && Objects.equals(this.waitId, other.waitId) && Objects.equals(this.queueName, other.queueName)
        && Objects.equals(this.status, other.status) && Objects.equals(this.delegateId, other.delegateId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("taskType", taskType)
        .add("parameters", parameters)
        .add("tag", tag)
        .add("accountId", accountId)
        .add("waitId", waitId)
        .add("queueName", queueName)
        .add("status", status)
        .add("delegateId", delegateId)
        .toString();
  }

  public DelegateRunnableTask getDelegateRunnableTask() {
    return delegateRunnableTask;
  }

  public void setDelegateRunnableTask(DelegateRunnableTask delegateRunnableTask) {
    this.delegateRunnableTask = delegateRunnableTask;
  }

  public static class Context {
    private String accountId;
    private String appId;
    private long timeOut = SYNC_CALL_TIMEOUT_INTERVAL;

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
     * Getter for property 'appId'.
     *
     * @return Value for property 'appId'.
     */
    public String getAppId() {
      return appId;
    }

    /**
     * Setter for property 'appId'.
     *
     * @param appId Value to set for property 'appId'.
     */
    public void setAppId(String appId) {
      this.appId = appId;
    }

    public long getTimeOut() {
      return timeOut;
    }

    public void setTimeOut(long timeOut) {
      this.timeOut = timeOut;
    }

    public static final class Builder {
      private String accountId;
      private String appId;

      private Builder() {}

      public static Builder aContext() {
        return new Builder();
      }

      public Builder withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
      }

      public Builder withAppId(String appId) {
        this.appId = appId;
        return this;
      }

      public Builder but() {
        return aContext().withAccountId(accountId).withAppId(appId);
      }

      public Context build() {
        Context context = new Context();
        context.setAccountId(accountId);
        context.setAppId(appId);
        return context;
      }
    }
  }

  public static class Converter extends TypeConverter {
    public Converter() {
      super(Object[].class);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
      return KryoUtils.asString(value);
    }

    @Override
    public Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
      return KryoUtils.asObject((String) fromDBObject);
    }
  }

  public enum Status { QUEUED, STARTED, FINISHED, ERROR, ABORTED }

  public static final class Builder {
    private TaskType taskType;
    private Object[] parameters;
    private String tag;
    private String accountId;
    private String waitId;
    private String queueName;
    private Status status = Status.QUEUED;
    private String delegateId;
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

    public Builder withQueueName(String queueName) {
      this.queueName = queueName;
      return this;
    }

    public Builder withStatus(Status status) {
      this.status = status;
      return this;
    }

    public Builder withDelegateId(String delegateId) {
      this.delegateId = delegateId;
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
          .withQueueName(queueName)
          .withStatus(status)
          .withDelegateId(delegateId)
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
      delegateTask.setQueueName(queueName);
      delegateTask.setStatus(status);
      delegateTask.setDelegateId(delegateId);
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
