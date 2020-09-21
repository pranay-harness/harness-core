/**
 *
 */

package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.beans.CreatedByType;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.WorkflowType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.artifact.Artifact;

import java.util.List;
import java.util.Map;

/**
 * The type Execution args.
 *
 * @author Rishi
 */
@FieldNameConstants(innerTypeName = "ExecutionArgsKeys")
@Getter
@Setter
public class ExecutionArgs {
  private WorkflowType workflowType;
  private String serviceId;
  private String commandName;
  private ExecutionStrategy executionStrategy;
  private List<Artifact> artifacts;
  private Map<String, String> artifactIdNames;
  private String orchestrationId;
  @Transient private List<ServiceInstance> serviceInstances;
  private Map<String, String> serviceInstanceIdNames;
  @Transient private ExecutionCredential executionCredential;
  private ErrorStrategy errorStrategy;
  private boolean triggeredFromPipeline;
  private String pipelineId;
  private String pipelinePhaseElementId;
  private int pipelinePhaseParallelIndex;
  private String stageName;
  private Map<String, String> workflowVariables;
  private String notes;
  @Deprecated @JsonIgnore private EmbeddedUser triggeredBy;
  private CreatedByType createdByType;
  private String triggeringApiKeyId;
  private boolean excludeHostsWithSameArtifact;
  private boolean notifyTriggeredUserOnly;
  private List<ArtifactVariable> artifactVariables;
  private boolean targetToSpecificHosts;
  private List<String> hosts;
  // If any variable is Runtime and Default values is provided
  private boolean continueWithDefaultValues;
}
