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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
  private Set<String> blacklistedDelegateIds = new HashSet<>();
  private Set<String> validatingDelegateIds = new HashSet<>();
  private Set<String> validationCompleteDelegateIds = new HashSet<>();

  @Transient private transient DelegateRunnableTask delegateRunnableTask;

  public boolean isTimedOut() {
    return getLastUpdatedAt() + timeout <= System.currentTimeMillis();
  }

  public TaskType getTaskType() {
    return taskType;
  }

  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  public Object[] getParameters() {
    return parameters;
  }

  public void setParameters(Object[] parameters) {
    this.parameters = parameters;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getWaitId() {
    return waitId;
  }

  public void setWaitId(String waitId) {
    this.waitId = waitId;
  }

  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getDelegateId() {
    return delegateId;
  }

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

  public DelegateRunnableTask getDelegateRunnableTask() {
    return delegateRunnableTask;
  }

  public void setDelegateRunnableTask(DelegateRunnableTask delegateRunnableTask) {
    this.delegateRunnableTask = delegateRunnableTask;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public boolean isAsync() {
    return async;
  }

  public void setAsync(boolean async) {
    this.async = async;
  }

  public Set<String> getBlacklistedDelegateIds() {
    return blacklistedDelegateIds;
  }

  public void setBlacklistedDelegateIds(Set<String> blacklistedDelegateIds) {
    this.blacklistedDelegateIds = blacklistedDelegateIds;
  }

  public Set<String> getValidatingDelegateIds() {
    return validatingDelegateIds;
  }

  public void setValidatingDelegateIds(Set<String> validatingDelegateIds) {
    this.validatingDelegateIds = validatingDelegateIds;
  }

  public Set<String> getValidationCompleteDelegateIds() {
    return validationCompleteDelegateIds;
  }

  public void setValidationCompleteDelegateIds(Set<String> validationCompleteDelegateIds) {
    this.validationCompleteDelegateIds = validationCompleteDelegateIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
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

  public static class SyncTaskContext {
    private String accountId;
    private String appId;
    private long timeout = DEFAULT_SYNC_CALL_TIMEOUT;

    public String getAccountId() {
      return accountId;
    }

    public void setAccountId(String accountId) {
      this.accountId = accountId;
    }

    public String getAppId() {
      return appId;
    }

    public void setAppId(String appId) {
      this.appId = appId;
    }

    public long getTimeout() {
      return timeout;
    }

    public void setTimeout(long timeout) {
      this.timeout = timeout;
    }

    public static final class Builder {
      private String accountId;
      private String appId;
      private long timeout = DEFAULT_SYNC_CALL_TIMEOUT;

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

      public Builder withTimeout(long timeout) {
        this.timeout = timeout;
        return this;
      }

      public Builder but() {
        return aContext().withAccountId(accountId).withAppId(appId).withTimeout(timeout);
      }

      public SyncTaskContext build() {
        SyncTaskContext syncTaskContext = new SyncTaskContext();
        syncTaskContext.setAccountId(accountId);
        syncTaskContext.setAppId(appId);
        syncTaskContext.setTimeout(timeout);
        return syncTaskContext;
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
