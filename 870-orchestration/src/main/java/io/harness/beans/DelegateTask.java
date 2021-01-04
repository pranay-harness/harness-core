package io.harness.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotation.HarnessEntity;
import io.harness.delegate.beans.DelegateTaskRank;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskData.TaskDataKeys;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.HDelegateTask;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.tasks.Cd1SetupFields;

import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@EqualsAndHashCode(exclude = {"uuid", "createdAt", "lastUpdatedAt", "validUntil"})
@Entity(value = "delegateTasks", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "DelegateTaskKeys")
public class DelegateTask
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, HDelegateTask {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("index")
                 .field(DelegateTaskKeys.status)
                 .field(DelegateTaskKeys.expiry)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("rebroadcast")
                 .field(DelegateTaskKeys.version)
                 .field(DelegateTaskKeys.status)
                 .field(DelegateTaskKeys.delegateId)
                 .field(DelegateTaskKeys.nextBroadcast)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("pulling")
                 .field(DelegateTaskKeys.accountId)
                 .field(DelegateTaskKeys.status)
                 .field(DelegateTaskKeys.data_async)
                 .field(DelegateTaskKeys.expiry)
                 .build())
        .build();
  }

  @NotNull private TaskData data;
  private List<ExecutionCapability> executionCapabilities;

  @Id private String uuid;
  @NotEmpty private String accountId;
  private String driverId;

  private DelegateTaskRank rank;

  private String description;
  private boolean selectionLogsTrackingEnabled;

  private String workflowExecutionId;

  @Singular private Map<String, String> setupAbstractions;

  /**
   * This field is intended to be used by Task owners to prepare key-value pairs which should represent the baseLogKey
   * to be used for log streaming. If any other sub-step, like command unit, exists and has to be logged in a dedicated
   * log stream, command unit identifier will be appended to the base key, by the logger implementation based on the
   * command unit identifier passed by the task that is being executed. SortedMap is used, so that the same order is
   * guarantied every time when the key is being built, on manager or delegate side.
   *
   * Convention for key generation will be the following:
   *
   *  [mapKey]:[mapvalue]-[mapKey]:[mapvalue]-...
   *
   *  In case there is a command unit, it should be appended to the end of the base key:
   *
   *  [mapKey]:[mapvalue]-[mapKey]:[mapvalue]-commandUnit:[commandUnitIdentifier]
   *
   * Example:
   *     key: pipelineId, value: 1111
   *     key: stageId, value: 2222
   *     key: stepId, value: 3333
   *
   * Value of the key would be: pipelineId:1111-stageId:2222-stepId:3333
   *
   * In case there is a command unit that requires a dedicated log stream, manager(while reading logs) and logger
   * implementation(while writing logs) should concatenate the commandUnit part to the end:
   *
   * Value of the key would be: pipelineId:1111-stageId:2222-stepId:3333-commandUnit:XYZ
   *
   */
  private LinkedHashMap<String, String> logStreamingAbstractions;

  private String version;

  // Please use SelectorCapability instead.
  @Deprecated private List<String> tags;

  // This extra field is pointless, we should use the task uuid.
  @Deprecated private String waitId;

  private long createdAt;
  private long lastUpdatedAt;

  private Status status;

  private Long validationStartedAt;
  private Set<String> validatingDelegateIds;
  private Set<String> validationCompleteDelegateIds;

  private String delegateId;
  private String preAssignedDelegateId;
  private Set<String> alreadyTriedDelegates;

  // Intended to be used for targeting a delegate for the purpose of delegate profile script execution
  private String mustExecuteOnDelegateId;

  private Long lastBroadcastAt;
  private int broadcastCount;
  private long nextBroadcast;

  private long expiry;

  @FdTtlIndex @Default private Date validUntil = Date.from(OffsetDateTime.now().plusDays(2).toInstant());

  // Following getters, setters have been added temporarily because of backward compatibility

  public String calcDescription() {
    if (isEmpty(description)) {
      return data.getTaskType();
    }
    return description;
  }

  @Deprecated
  /**
   * @deprecated Value should be moved to setupAbstractions map and read from there
   */
  public String getAppId() {
    return setupAbstractions == null ? null : setupAbstractions.get(Cd1SetupFields.APP_ID_FIELD);
  }

  @Deprecated
  /**
   * @deprecated Value should be moved to setupAbstractions map and read from there
   */
  public String getEnvId() {
    return setupAbstractions == null ? null : setupAbstractions.get(Cd1SetupFields.ENV_ID_FIELD);
  }

  @Deprecated
  /**
   * @deprecated Value should be moved to setupAbstractions map and read from there
   */
  public String getInfrastructureMappingId() {
    return setupAbstractions == null ? null : setupAbstractions.get(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD);
  }

  @Deprecated
  /**
   * @deprecated Value should be moved to setupAbstractions map and read from there
   */
  public String getServiceTemplateId() {
    return setupAbstractions == null ? null : setupAbstractions.get(Cd1SetupFields.SERVICE_TEMPLATE_ID_FIELD);
  }

  @Deprecated
  /**
   * @deprecated Value should be moved to setupAbstractions map and read from there
   */
  public String getArtifactStreamId() {
    return setupAbstractions == null ? null : setupAbstractions.get(Cd1SetupFields.ARTIFACT_STREAM_ID_FIELD);
  }

  public enum Status {
    QUEUED,
    STARTED,
    ERROR,
    ABORTED,
    PARKED;

    private static Set<Status> finalStatuses = EnumSet.of(ERROR, ABORTED);
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
