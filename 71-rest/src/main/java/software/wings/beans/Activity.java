package software.wings.beans;

import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.beans.TriggeredBy.triggeredBy;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Version;
import org.mongodb.morphia.utils.IndexType;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.Activity.ActivityKeys;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ActivityKeys")
@NoArgsConstructor
@AllArgsConstructor
@Entity(value = "activities", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Indexes(@Index(options = @IndexOptions(name = "app_status_createdAt"),
    fields =
    {
      @Field(value = ActivityKeys.appId)
      , @Field(value = ActivityKeys.serviceInstanceId), @Field(value = ActivityKeys.status),
          @Field(value = ActivityKeys.createdAt, type = IndexType.DESC)
    }))
public class Activity
    implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @Indexed @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @Indexed private long createdAt;

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

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
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

  @NotNull
  public Activity with(final @NotNull ExecutionContext executionContext) {
    final Application app = executionContext.fetchRequiredApp();
    final Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    final WorkflowStandardParams workflowStandardParams =
        executionContext.getContextElement(ContextElementType.STANDARD);
    final EmbeddedUser currentUser = workflowStandardParams.getCurrentUser();
    final InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", currentUser, USER);

    this.appId = app.getUuid();
    this.applicationName = app.getName();
    this.type = Type.Verification;
    this.workflowType = executionContext.getWorkflowType();
    this.workflowExecutionName = executionContext.getWorkflowExecutionName();
    this.stateExecutionInstanceId = executionContext.getStateExecutionInstanceId();
    this.stateExecutionInstanceName = executionContext.getStateExecutionInstanceName();
    this.workflowId = executionContext.getWorkflowId();
    this.workflowExecutionId = executionContext.getWorkflowExecutionId();
    this.commandUnits = Collections.emptyList();
    this.status = RUNNING;
    this.triggeredBy = triggeredBy(currentUser.getName(), currentUser.getEmail());

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType() == BUILD) {
      this.environmentId = GLOBAL_ENV_ID;
      this.environmentName = GLOBAL_ENV_ID;
      this.environmentType = ALL;
    } else if (env != null) {
      this.environmentId = env.getUuid();
      this.environmentName = env.getName();
      this.environmentType = env.getEnvironmentType();
    }
    if (instanceElement != null) {
      final ServiceTemplateElement serviceTemplateElement = instanceElement.getServiceTemplateElement();
      this.serviceTemplateId = serviceTemplateElement.getUuid();
      this.serviceTemplateName = serviceTemplateElement.getName();
      this.serviceId = serviceTemplateElement.getServiceElement().getUuid();
      this.serviceName = serviceTemplateElement.getServiceElement().getName();
      this.serviceInstanceId = instanceElement.getUuid();
      this.hostName = instanceElement.getHost().getHostName();
    }

    return this;
  }

  @NotNull
  public Activity with(@NotNull final State state) {
    this.commandName = state.getName();
    this.commandType = state.getStateType();
    return this;
  }

  @NotNull
  public Activity with(@NotNull final CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
    return this;
  }
}
