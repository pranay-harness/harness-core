package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.beans.EmbeddedUser;
import io.harness.delegate.task.DelegateRunnableTask;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.serializer.KryoUtils;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import software.wings.beans.DelegateTask.Converter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "delegateTasks", noClassnameStored = true)
@Converters(Converter.class)
public class DelegateTask implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static final String APP_ID_KEY = "appId";

  public static final long DEFAULT_SYNC_CALL_TIMEOUT = 60 * 1000; // 1 minute

  public static final long DEFAULT_ASYNC_CALL_TIMEOUT = 10 * 60 * 1000; // 10 minutes

  @Id private String uuid;
  @Indexed protected String appId;
  private long createdAt;
  private long lastUpdatedAt;

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
  private String preAssignedDelegateId;
  private Set<String> alreadyTriedDelegates = new HashSet<>();
  private String serviceTemplateId;
  private String artifactStreamId;
  private String correlationId;

  @Transient private transient ResponseData notifyResponse;
  @Transient private transient DelegateRunnableTask delegateRunnableTask;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(2).toInstant());

  public boolean isTimedOut() {
    return getLastUpdatedAt() + timeout <= System.currentTimeMillis();
  }

  public ResponseData getNotifyResponse() {
    if (notifyResponse != null) {
      return notifyResponse;
    }
    if (serializedNotifyResponseData != null) {
      return (ResponseData) KryoUtils.asObject(serializedNotifyResponseData);
    }
    return null;
  }

  public void setNotifyResponse(ResponseData notifyResponse) {
    this.notifyResponse = notifyResponse;
    setSerializedNotifyResponseData(notifyResponse != null ? KryoUtils.asBytes(notifyResponse) : null);
  }

  @Value
  @lombok.Builder
  public static class SyncTaskContext {
    private String accountId;
    private String appId;
    private String envId;
    private String infrastructureMappingId;
    private long timeout;
    private List<String> tags;
    private String correlationId;
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
    private String taskType;
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
    private ResponseData notifyResponse;
    private String preAssignedDelegateId;
    private String serviceTemplateId;
    private String artifactStreamId;
    private String correlationId;

    private Builder() {}

    public static Builder aDelegateTask() {
      return new Builder();
    }

    public Builder withVersion(String version) {
      this.version = version;
      return this;
    }

    public Builder withTaskType(String taskType) {
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

    public Builder withNotifyResponse(ResponseData notifyResponse) {
      this.notifyResponse = notifyResponse;
      return this;
    }

    public Builder withPreAssignedDelegateId(String preAssignedDelegateId) {
      this.preAssignedDelegateId = preAssignedDelegateId;
      return this;
    }

    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public Builder withArtifactStreamId(String artifactStreamId) {
      this.artifactStreamId = artifactStreamId;
      return this;
    }

    public Builder withCorrelationId(String correlationId) {
      this.correlationId = correlationId;
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
          .withNotifyResponse(notifyResponse)
          .withPreAssignedDelegateId(preAssignedDelegateId)
          .withServiceTemplateId(serviceTemplateId)
          .withArtifactStreamId(artifactStreamId)
          .withCorrelationId(correlationId);
    }

    public DelegateTask build() {
      DelegateTask delegateTask = new DelegateTask();
      delegateTask.setVersion(version);
      delegateTask.setTaskType(taskType);
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
      delegateTask.setLastUpdatedAt(lastUpdatedAt);
      delegateTask.setNotifyResponse(notifyResponse);
      delegateTask.setPreAssignedDelegateId(preAssignedDelegateId);
      delegateTask.setServiceTemplateId(serviceTemplateId);
      delegateTask.setArtifactStreamId(artifactStreamId);
      delegateTask.setCorrelationId(correlationId);
      return delegateTask;
    }
  }
}
