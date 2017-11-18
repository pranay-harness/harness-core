package software.wings.beans;

import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Version;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.sm.ExecutionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Entity(value = "activities", noClassnameStored = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity extends Base {
  private Type type;
  @NotEmpty private String applicationName;
  @NotEmpty private String environmentId;
  @NotEmpty private String environmentName;
  @NotNull private EnvironmentType environmentType;
  @NotEmpty private String commandName;
  @NotNull private List<CommandUnit> commandUnits = new ArrayList<>();
  private Map<String, Integer> commandNameVersionMap;
  private Map<String, String> serviceVariables = Maps.newHashMap();
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

  /**
   * The enum Type.
   */
  public enum Type {
    /**
     * Command type.
     */
    Command, /**
              * Verification type.
              */
    Verification
  }
}
