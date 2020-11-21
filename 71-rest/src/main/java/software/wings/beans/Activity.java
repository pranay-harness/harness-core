package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.beans.WorkflowType;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import software.wings.beans.Activity.ActivityKeys;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.entityinterface.ApplicationAccess;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Version;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ActivityKeys")
@NoArgsConstructor
@AllArgsConstructor
@Entity(value = "activities", noClassnameStored = true)
@HarnessEntity(exportable = false)
@CdIndex(name = "app_status_createdAt",
    fields =
    {
      @Field(value = ActivityKeys.appId)
      , @Field(value = ActivityKeys.serviceInstanceId), @Field(value = ActivityKeys.status),
          @Field(value = ActivityKeys.createdAt, type = IndexType.DESC)
    })
public class Activity implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                 UpdatedByAware, ApplicationAccess {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @FdIndex @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @FdIndex private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  private Type type;
  @NotEmpty private String applicationName;
  @NotEmpty private String environmentId;
  @NotEmpty private String environmentName;
  @NotNull private EnvironmentType environmentType;
  @NotEmpty private String commandName;
  @Default @NotNull private List<CommandUnit> commandUnits = new ArrayList<>();
  private Map<String, Integer> commandNameVersionMap;
  private String commandType;
  private String serviceId;
  private String serviceName;
  private String serviceTemplateId;
  private String serviceTemplateName;
  private String hostName;
  private String publicDns;
  private String serviceInstanceId;
  @NotEmpty private String workflowExecutionId;
  @NotEmpty private String workflowId;
  @NotEmpty private String workflowExecutionName;
  @NotNull private WorkflowType workflowType;
  @NotEmpty private String stateExecutionInstanceId;
  @NotEmpty private String stateExecutionInstanceName;
  @Version private Long version; // Morphia managed for optimistic locking. don't remove

  @Default private CommandUnitType commandUnitType = CommandUnitType.COMMAND;
  private boolean logPurged;

  private String artifactStreamId;
  private String artifactStreamName;
  private boolean isPipeline;
  private String artifactId;
  private String artifactName;
  @Default private ExecutionStatus status = ExecutionStatus.RUNNING;
  private TriggeredBy triggeredBy;
  @FdIndex private String accountId;

  @JsonIgnore
  @SchemaIgnore
  @FdTtlIndex
  @Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  /**
   * The enum Type.
   */
  public enum Type {
    /**
     * Command type.
     */
    Command,
    /**
     * Verification type.
     */
    Verification,
    /**
     * None of the above.
     */
    Other
  }
}
