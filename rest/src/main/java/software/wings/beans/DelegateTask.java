package software.wings.beans;

import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.common.Constants.DEFAULT_SYNC_CALL_TIMEOUT;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import software.wings.beans.DelegateTask.Converter;
import software.wings.delegatetasks.DelegateRunnableTask;
import software.wings.utils.KryoUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "delegateTasks", noClassnameStored = true)
@Converters(Converter.class)
public class DelegateTask extends Base {
  private String version;
  @NotNull private String taskType;
  private Object[] parameters;
  private List<String> tags = new ArrayList<>();
  @NotEmpty private String accountId;
  private String waitId;
  private Status status = Status.QUEUED;
  private String delegateId;
  private long timeout = DEFAULT_ASYNC_CALL_TIMEOUT;
  private boolean async = true;
  private String envId;
  private String infrastructureMappingId;
  private Long validationStartedAt;
  private Long lastBroadcastAt;
  private int broadcastCount;
  private Set<String> validatingDelegateIds = new HashSet<>();
  private Set<String> validationCompleteDelegateIds = new HashSet<>();
  private byte[] serializedNotifyResponseData;

  @Transient private transient NotifyResponseData notifyResponse;
  @Transient private transient DelegateRunnableTask delegateRunnableTask;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(2).toInstant());

  public boolean isTimedOut() {
    return getLastUpdatedAt() + timeout <= System.currentTimeMillis();
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Object[] getParameters() {
    return parameters;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setParameters(Object[] parameters) {
    this.parameters = parameters;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
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

  public Long getValidationStartedAt() {
    return validationStartedAt;
  }

  public void setValidationStartedAt(Long validationStartedAt) {
    this.validationStartedAt = validationStartedAt;
  }

  public Long getLastBroadcastAt() {
    return lastBroadcastAt;
  }

  public void setLastBroadcastAt(Long lastBroadcastAt) {
    this.lastBroadcastAt = lastBroadcastAt;
  }

  public int getBroadcastCount() {
    return broadcastCount;
  }

  public void setBroadcastCount(int broadcastCount) {
    this.broadcastCount = broadcastCount;
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

  public NotifyResponseData getNotifyResponse() {
    if (notifyResponse != null) {
      return notifyResponse;
    }
    if (serializedNotifyResponseData != null) {
      return (NotifyResponseData) KryoUtils.asObject(serializedNotifyResponseData);
    }
    return null;
  }

  public void setNotifyResponse(NotifyResponseData notifyResponse) {
    this.notifyResponse = notifyResponse;
    setSerializedNotifyResponseData(notifyResponse != null ? KryoUtils.asBytes(notifyResponse) : null);
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getSerializedNotifyResponseData() {
    return serializedNotifyResponseData;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setSerializedNotifyResponseData(byte[] serializedNotifyResponseData) {
    this.serializedNotifyResponseData = serializedNotifyResponseData;
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
    return timeout == that.timeout && async == that.async && Objects.equals(version, that.version)
        && Objects.equals(taskType, that.taskType) && Arrays.equals(parameters, that.parameters)
        && Objects.equals(tags, that.tags) && Objects.equals(accountId, that.accountId)
        && Objects.equals(waitId, that.waitId) && status == that.status && Objects.equals(delegateId, that.delegateId)
        && Objects.equals(envId, that.envId) && Objects.equals(infrastructureMappingId, that.infrastructureMappingId)
        && Objects.equals(delegateRunnableTask, that.delegateRunnableTask)
        && Objects.equals(notifyResponse, that.notifyResponse)
        && Arrays.equals(serializedNotifyResponseData, that.serializedNotifyResponseData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), version, taskType, parameters, tags, accountId, waitId, status, delegateId,
        timeout, async, envId, infrastructureMappingId, delegateRunnableTask, notifyResponse,
        serializedNotifyResponseData);
  }

  @Override
  public String toString() {
    return "DelegateTask{"
        + "version='" + version + '\'' + ", taskType=" + taskType + ", parameters=" + Arrays.toString(parameters)
        + ", tag='" + tags + '\'' + ", accountId='" + accountId + '\'' + ", waitId='" + waitId + '\'' + '\''
        + ", status=" + status + ", delegateId='" + delegateId + '\'' + ", timeout=" + timeout + ", async=" + async
        + ", envId='" + envId + '\'' + ", infrastructureMappingId='" + infrastructureMappingId + '\''
        + ", delegateRunnableTask=" + delegateRunnableTask + ", notifyResponse=" + notifyResponse + '}';
  }

  public static class SyncTaskContext {
    private String accountId;
    private String appId;
    private String envId;
    private String infrastructureMappingId;
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

    public String getInfrastructureMappingId() {
      return infrastructureMappingId;
    }

    public void setInfrastructureMappingId(String infrastructureMappingId) {
      this.infrastructureMappingId = infrastructureMappingId;
    }

    public String getEnvId() {
      return envId;
    }

    public void setEnvId(String envId) {
      this.envId = envId;
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
      private String envId;
      private String infrastructureMappingId;
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

      public Builder withInfrastructureMappingId(String infrastructureMappingId) {
        this.infrastructureMappingId = infrastructureMappingId;
        return this;
      }

      public Builder withEnvId(String envId) {
        this.envId = envId;
        return this;
      }

      public Builder withTimeout(long timeout) {
        this.timeout = timeout;
        return this;
      }

      public Builder but() {
        return aContext()
            .withAccountId(accountId)
            .withAppId(appId)
            .withEnvId(envId)
            .withTimeout(timeout)
            .withInfrastructureMappingId(infrastructureMappingId);
      }

      public SyncTaskContext build() {
        SyncTaskContext syncTaskContext = new SyncTaskContext();
        syncTaskContext.setAccountId(accountId);
        syncTaskContext.setAppId(appId);
        syncTaskContext.setEnvId(envId);
        syncTaskContext.setInfrastructureMappingId(infrastructureMappingId);
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
    private String version;
    private TaskType taskType;
    private Object[] parameters;
    private List<String> tags;
    private String accountId;
    private String waitId;
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
    private NotifyResponseData notifyResponse;

    private Builder() {}

    public static Builder aDelegateTask() {
      return new Builder();
    }

    public Builder withVersion(String version) {
      this.version = version;
      return this;
    }

    public Builder withTaskType(TaskType taskType) {
      this.taskType = taskType;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withParameters(Object[] parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder withTags(List<String> tags) {
      this.tags = tags;
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

    public Builder withNotifyResponse(NotifyResponseData notifyResponse) {
      this.notifyResponse = notifyResponse;
      return this;
    }

    public Builder but() {
      return aDelegateTask()
          .withVersion(version)
          .withTaskType(taskType)
          .withParameters(parameters)
          .withTags(tags)
          .withAccountId(accountId)
          .withWaitId(waitId)
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
          .withLastUpdatedAt(lastUpdatedAt)
          .withNotifyResponse(notifyResponse);
    }

    public DelegateTask build() {
      DelegateTask delegateTask = new DelegateTask();
      delegateTask.setVersion(version);
      delegateTask.setTaskType(taskType != null ? taskType.name() : null);
      delegateTask.setParameters(parameters);
      delegateTask.setTags(tags);
      delegateTask.setAccountId(accountId);
      delegateTask.setWaitId(waitId);
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
      delegateTask.setNotifyResponse(notifyResponse);
      return delegateTask;
    }
  }
}
