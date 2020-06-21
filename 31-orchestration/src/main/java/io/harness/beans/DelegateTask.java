package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.DelegateTask.ParametersConverter;
import io.harness.beans.DelegateTask.ResponseDataConverter;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskData.TaskDataKeys;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.mongo.KryoConverter;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.IndexOptions;
import io.harness.mongo.index.Indexed;
import io.harness.mongo.index.Indexes;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.tasks.Task;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.converters.SimpleValueConverter;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(exclude = {"uuid", "createdAt", "lastUpdatedAt", "validUntil"})
@Entity(value = "delegateTasks", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Converters({ParametersConverter.class, ResponseDataConverter.class})
@FieldNameConstants(innerTypeName = "DelegateTaskKeys")
@Indexes({
  @Index(name = "index", fields = { @Field(DelegateTaskKeys.status)
                                    , @Field(DelegateTaskKeys.expiry) })
  , @Index(name = "pulling", fields = {
    @Field(DelegateTaskKeys.accountId)
    , @Field(DelegateTaskKeys.status), @Field("data.async"), @Field(DelegateTaskKeys.expiry),
  }), @Index(name = "data_async", fields = { @Field("data.async") })
})
public class DelegateTask implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, Task {
  public static final String TASK_IDENTIFIER = "DELEGATE_TASK";

  // TODO: this is temporary to propagate if the compatibility framework is enabled for particular task
  private boolean capabilityFrameworkEnabled;

  @NotNull private TaskData data;
  private List<ExecutionCapability> executionCapabilities;

  @Id private String uuid;
  @NotEmpty private String accountId;
  private boolean selectionLogsTrackingEnabled;

  protected String appId;
  private String envId;
  private String infrastructureMappingId;
  private String serviceTemplateId;
  private String artifactStreamId;
  private String workflowExecutionId;

  private Map<String, String> setupAbstractions;

  private String version;
  private List<String> tags;

  private String waitId;
  private String correlationId;

  private long createdAt;
  private long lastUpdatedAt;

  private Status status;
  private ResponseData notifyResponse;

  private Long validationStartedAt;
  private Set<String> validatingDelegateIds;
  private Set<String> validationCompleteDelegateIds;

  private String delegateId;
  private String preAssignedDelegateId;
  private Set<String> alreadyTriedDelegates;

  private Long lastBroadcastAt;
  private int broadcastCount;
  private long nextBroadcast;

  private long expiry;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  @Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(2).toInstant());

  @Override
  @JsonIgnore
  @Nonnull
  public String getTaskIdentifier() {
    return TASK_IDENTIFIER;
  }

  @Override
  @JsonIgnore
  @Nonnull
  public String getTaskType() {
    return data.getTaskType();
  }

  public static class ParametersConverter extends KryoConverter {
    public ParametersConverter() {
      super(Object[].class);
    }
  }

  public static class ResponseDataConverter extends KryoConverter implements SimpleValueConverter {
    public ResponseDataConverter() {
      super(ResponseData.class);
    }
  }

  public enum Status {
    QUEUED,
    STARTED,
    FINISHED,
    ERROR,
    ABORTED;

    private static Set<Status> finalStatuses = EnumSet.of(FINISHED, ERROR, ABORTED);
    private static Set<Status> runningStatuses = EnumSet.of(QUEUED, STARTED);

    public static Set<Status> finalStatuses() {
      return finalStatuses;
    }

    public static boolean isFinalStatus(Status status) {
      return status != null && finalStatuses.contains(status);
    }

    public static Set<Status> runningStatuses() {
      return runningStatuses;
    }

    public static boolean isRunningStatus(Status status) {
      return status != null && runningStatuses.contains(status);
    }
  }

  @UtilityClass
  public static final class DelegateTaskKeys {
    public static final String data_parameters = data + "." + TaskDataKeys.parameters;
    public static final String data_taskType = data + "." + TaskDataKeys.taskType;
    public static final String data_timeout = data + "." + TaskDataKeys.timeout;
    public static final String data_async = data + "." + TaskDataKeys.async;
  }
}
