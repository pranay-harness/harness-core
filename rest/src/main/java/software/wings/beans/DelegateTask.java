package software.wings.beans;

import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.common.Constants.DEFAULT_SYNC_CALL_TIMEOUT;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.AlsoLoad;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import software.wings.beans.DelegateTask.Converter;
import software.wings.delegatetasks.DelegateRunnableTask;
import software.wings.utils.KryoUtils;

import java.util.Arrays;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
@Entity(value = "delegateTasks", noClassnameStored = true)
@Converters(Converter.class)
public class DelegateTask extends Base {
  @NotNull private TaskType taskType;
  private Object[] parameters;
  private String tag;
  @NotEmpty private String accountId;
  private String waitId;
  @AlsoLoad("topicName") private String queueName;
  private Status status = Status.QUEUED;
  private String delegateId;
  private long timeout = DEFAULT_ASYNC_CALL_TIMEOUT;
  private boolean async = true;
  private String envId;
  private String infrastructureMappingId;

  @Transient private transient DelegateRunnableTask delegateRunnableTask;

  /**
   * Is timed out boolean.
   *
   * @return the boolean
   */
  public boolean isTimedOut() {
    return getLastUpdatedAt() + timeout <= System.currentTimeMillis();
  }

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

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public String getInfrastructureMappingId() {
    return infrastructureMappingId;
  }

  public void setInfrastructureMappingId(String infrastructureMappingId) {
    this.infrastructureMappingId = infrastructureMappingId;
  }

  /**
   * Gets delegate runnable task.
   *
   * @return the delegate runnable task
   */
  public DelegateRunnableTask getDelegateRunnableTask() {
    return delegateRunnableTask;
  }

  /**
   * Sets delegate runnable task.
   *
   * @param delegateRunnableTask the delegate runnable task
   */
  public void setDelegateRunnableTask(DelegateRunnableTask delegateRunnableTask) {
    this.delegateRunnableTask = delegateRunnableTask;
  }

  /**
   * Gets timeout.
   *
   * @return the timeout
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * Sets timeout.
   *
   * @param timeout the timeout
   */
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  /**
   * Is async boolean.
   *
   * @return the boolean
   */
  public boolean isAsync() {
    return async;
  }

  /**
   * Sets async.
   *
   * @param async the async
   */
  public void setAsync(boolean async) {
    this.async = async;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    DelegateTask that = (DelegateTask) o;
    return timeout == that.timeout && async == that.async && taskType == that.taskType
        && Arrays.equals(parameters, that.parameters) && Objects.equals(tag, that.tag)
        && Objects.equals(accountId, that.accountId) && Objects.equals(waitId, that.waitId)
        && Objects.equals(queueName, that.queueName) && status == that.status
        && Objects.equals(delegateId, that.delegateId) && Objects.equals(envId, that.envId)
        && Objects.equals(infrastructureMappingId, that.infrastructureMappingId)
        && Objects.equals(delegateRunnableTask, that.delegateRunnableTask);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), taskType, parameters, tag, accountId, waitId, queueName, status, delegateId,
        timeout, async, envId, infrastructureMappingId, delegateRunnableTask);
  }

  @Override
  public String toString() {
    return "DelegateTask{"
        + "taskType=" + taskType + ", parameters=" + Arrays.toString(parameters) + ", tag='" + tag + '\''
        + ", accountId='" + accountId + '\'' + ", waitId='" + waitId + '\'' + ", queueName='" + queueName + '\''
        + ", status=" + status + ", delegateId='" + delegateId + '\'' + ", timeout=" + timeout + ", async=" + async
        + ", envId='" + envId + '\'' + ", infrastructureMappingId='" + infrastructureMappingId + '\''
        + ", delegateRunnableTask=" + delegateRunnableTask + '}';
  }

  /**
   * The type Context.
   */
  public static class SyncTaskContext {
    private String accountId;
    private String appId;
    private long timeOut = DEFAULT_SYNC_CALL_TIMEOUT;

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

    /**
     * Gets time out.
     *
     * @return the time out
     */
    public long getTimeOut() {
      return timeOut;
    }

    /**
     * Sets time out.
     *
     * @param timeOut the time out
     */
    public void setTimeOut(long timeOut) {
      this.timeOut = timeOut;
    }

    /**
     * The type Builder.
     */
    public static final class Builder {
      private String accountId;
      private String appId;

      private Builder() {}

      /**
       * A context builder.
       *
       * @return the builder
       */
      public static Builder aContext() {
        return new Builder();
      }

      /**
       * With account id builder.
       *
       * @param accountId the account id
       * @return the builder
       */
      public Builder withAccountId(String accountId) {
        this.accountId = accountId;
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
       * But builder.
       *
       * @return the builder
       */
      public Builder but() {
        return aContext().withAccountId(accountId).withAppId(appId);
      }

      /**
       * Build context.
       *
       * @return the context
       */
      public SyncTaskContext build() {
        SyncTaskContext syncTaskContext = new SyncTaskContext();
        syncTaskContext.setAccountId(accountId);
        syncTaskContext.setAppId(appId);
        return syncTaskContext;
      }
    }
  }

  /**
   * The type Converter.
   */
  public static class Converter extends TypeConverter {
    /**
     * Instantiates a new Converter.
     */
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

  /**
   * The enum Status.
   */
  public enum Status {
    /**
     * Queued status.
     */
    QUEUED, /**
             * Started status.
             */
    STARTED, /**
              * Finished status.
              */
    FINISHED, /**
               * Error status.
               */
    ERROR, /**
            * Aborted status.
            */
    ABORTED
  }

  public static final class Builder {
    private TaskType taskType;
    private Object[] parameters;
    private String tag;
    private String accountId;
    private String waitId;
    private String queueName;
    private Status status = Status.QUEUED;
    private String delegateId;
    private long timeout = DEFAULT_ASYNC_CALL_TIMEOUT;
    private boolean async = true;
    private String envId;
    private String uuid;
    private String infrastructureMappingId;
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

    public Builder withTimeout(long timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder withAsync(boolean async) {
      this.async = async;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withInfrastructureMappingId(String infrastructureMappingId) {
      this.infrastructureMappingId = infrastructureMappingId;
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
          .withTimeout(timeout)
          .withAsync(async)
          .withEnvId(envId)
          .withUuid(uuid)
          .withInfrastructureMappingId(infrastructureMappingId)
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
      delegateTask.setTimeout(timeout);
      delegateTask.setAsync(async);
      delegateTask.setEnvId(envId);
      delegateTask.setUuid(uuid);
      delegateTask.setInfrastructureMappingId(infrastructureMappingId);
      delegateTask.setAppId(appId);
      delegateTask.setCreatedBy(createdBy);
      delegateTask.setCreatedAt(createdAt);
      delegateTask.setLastUpdatedBy(lastUpdatedBy);
      delegateTask.setLastUpdatedAt(lastUpdatedAt);
      return delegateTask;
    }
  }
}
