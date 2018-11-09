package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Version;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.sm.ExecutionStatus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Entity(value = "activities", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class Activity extends Base {
  public static final String ARTIFACT_ID_KEY = "artifactId";
  public static final String SERVICE_INSTANCE_ID_KEY = "serviceInstanceId";
  public static final String STATUS_KEY = "status";
  public static final String WORKFLOW_EXECUTION_ID_KEY = "workflowExecutionId";

  private Type type;
  @NotEmpty private String applicationName;
  @NotEmpty private String environmentId;
  @NotEmpty private String environmentName;
  @NotNull private EnvironmentType environmentType;
  @NotEmpty private String commandName;
  @NotNull private List<CommandUnit> commandUnits = new ArrayList<>();
  private Map<String, Integer> commandNameVersionMap;
  // TODO: remove
  @Deprecated private Map<String, String> serviceVariables;
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

  private CommandUnitType commandUnitType = CommandUnitType.COMMAND;
  private boolean logPurged;

  private String artifactStreamId;
  private String artifactStreamName;
  private boolean isPipeline;
  private String artifactId;
  private String artifactName;
  private ExecutionStatus status = ExecutionStatus.RUNNING;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
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

  @Builder
  public Activity(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, Type type, String applicationName,
      String environmentId, String environmentName, EnvironmentType environmentType, String commandName,
      List<CommandUnit> commandUnits, Map<String, Integer> commandNameVersionMap, String commandType, String serviceId,
      String serviceName, String serviceTemplateId, String serviceTemplateName, String hostName, String publicDns,
      String serviceInstanceId, String workflowExecutionId, String workflowId, String workflowExecutionName,
      WorkflowType workflowType, String stateExecutionInstanceId, String stateExecutionInstanceName, Long version,
      CommandUnitType commandUnitType, boolean logPurged, String artifactStreamId, String artifactStreamName,
      boolean isPipeline, String artifactId, String artifactName, ExecutionStatus status) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.type = type;
    this.applicationName = applicationName;
    this.environmentId = environmentId;
    this.environmentName = environmentName;
    this.environmentType = environmentType;
    this.commandName = commandName;
    this.commandUnits = commandUnits == null ? new ArrayList<>() : commandUnits;
    this.commandNameVersionMap = commandNameVersionMap;
    this.commandType = commandType;
    this.serviceId = serviceId;
    this.serviceName = serviceName;
    this.serviceTemplateId = serviceTemplateId;
    this.serviceTemplateName = serviceTemplateName;
    this.hostName = hostName;
    this.publicDns = publicDns;
    this.serviceInstanceId = serviceInstanceId;
    this.workflowExecutionId = workflowExecutionId;
    this.workflowId = workflowId;
    this.workflowExecutionName = workflowExecutionName;
    this.workflowType = workflowType;
    this.stateExecutionInstanceId = stateExecutionInstanceId;
    this.stateExecutionInstanceName = stateExecutionInstanceName;
    this.version = version;
    this.commandUnitType = commandUnitType == null ? CommandUnitType.COMMAND : commandUnitType;
    this.logPurged = logPurged;
    this.artifactStreamId = artifactStreamId;
    this.artifactStreamName = artifactStreamName;
    this.isPipeline = isPipeline;
    this.artifactId = artifactId;
    this.artifactName = artifactName;
    this.status = status == null ? ExecutionStatus.RUNNING : status;
  }
}
