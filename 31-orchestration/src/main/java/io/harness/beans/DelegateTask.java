package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.Task;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.DelegateTask.ParametersConverter;
import io.harness.beans.DelegateTask.ResponseDataConverter;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskData.TaskDataKeys;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.mongo.KryoConverter;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.tasks.Cd1SetupFields;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
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

@CdIndex(name = "index", fields = { @Field(DelegateTaskKeys.status)
                                    , @Field(DelegateTaskKeys.expiry) })
@CdIndex(name = "pulling",
    fields =
    {
      @Field(DelegateTaskKeys.accountId)
      , @Field(DelegateTaskKeys.status), @Field("data.async"), @Field(DelegateTaskKeys.expiry),
    })
@CdIndex(name = "data_async", fields = { @Field("data.async") })
public class DelegateTask implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, Task {
  public static final String TASK_IDENTIFIER = "DELEGATE_TASK";

  // TODO: this is temporary to propagate if the compatibility framework is enabled for particular task
  private boolean capabilityFrameworkEnabled;

  @NotNull private TaskData data;
  private List<ExecutionCapability> executionCapabilities;

  @Id private String uuid;
  @NotEmpty private String accountId;
  private boolean selectionLogsTrackingEnabled;

  private String workflowExecutionId;

  @Singular private Map<String, String> setupAbstractions;

  private String version;
  private List<String> tags;

  private String waitId;

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

  @FdTtlIndex @Default private Date validUntil = Date.from(OffsetDateTime.now().plusDays(2).toInstant());

  // Following getters, setters have been added temporarily because of backward compatibility

  @Deprecated
  /**
   * @deprecated Value should be moved to setupAbstractions map and read from there
   */
  public String getAppId() {
    return setupAbstractions.get(Cd1SetupFields.APP_ID_FIELD);
  }

  @Deprecated
  /**
   * @deprecated Value should be moved to setupAbstractions map and read from there
   */
  public String getEnvId() {
    return setupAbstractions.get(Cd1SetupFields.ENV_ID_FIELD);
  }

  @Deprecated
  /**
   * @deprecated Value should be moved to setupAbstractions map and read from there
   */
  public String getInfrastructureMappingId() {
    return setupAbstractions.get(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD);
  }

  @Deprecated
  /**
   * @deprecated Value should be moved to setupAbstractions map and read from there
   */
  public String getServiceTemplateId() {
    return setupAbstractions.get(Cd1SetupFields.SERVICE_TEMPLATE_ID_FIELD);
  }

  @Deprecated
  /**
   * @deprecated Value should be moved to setupAbstractions map and read from there
   */
  public String getArtifactStreamId() {
    return setupAbstractions.get(Cd1SetupFields.ARTIFACT_STREAM_ID_FIELD);
  }

  @Override
  @JsonIgnore
  @Nonnull
  public String getTaskIdentifier() {
    return TASK_IDENTIFIER;
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
