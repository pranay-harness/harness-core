/**
 *
 */

package software.wings.beans;

import java.util.List;

/**
 * @author Rishi
 */
public class ExecutionArgs {
  private OrchestrationType orchestrationType;

  ;
  private String serviceId;
  private String commandName;
  private ExecutionStrategy executionStrategy;
  private String releaseId;
  private List<String> artifactIds;
  private String orchestrationId;
  private List<String> serviceInstanceIds;
  private ExecutionCredential executionCredential;

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public String getReleaseId() {
    return releaseId;
  }

  public void setReleaseId(String releaseId) {
    this.releaseId = releaseId;
  }

  public List<String> getArtifactIds() {
    return artifactIds;
  }

  public void setArtifactIds(List<String> artifactIds) {
    this.artifactIds = artifactIds;
  }

  public OrchestrationType getOrchestrationType() {
    return orchestrationType;
  }

  public void setOrchestrationType(OrchestrationType orchestrationType) {
    this.orchestrationType = orchestrationType;
  }

  public String getOrchestrationId() {
    return orchestrationId;
  }

  public void setOrchestrationId(String orchestrationId) {
    this.orchestrationId = orchestrationId;
  }

  public ExecutionCredential getExecutionCredential() {
    return executionCredential;
  }

  public void setExecutionCredential(ExecutionCredential executionCredential) {
    this.executionCredential = executionCredential;
  }

  public ExecutionStrategy getExecutionStrategy() {
    return executionStrategy;
  }

  public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
    this.executionStrategy = executionStrategy;
  }

  public List<String> getServiceInstanceIds() {
    return serviceInstanceIds;
  }

  public void setServiceInstanceIds(List<String> serviceInstanceIds) {
    this.serviceInstanceIds = serviceInstanceIds;
  }

  public enum OrchestrationType { ORCHESTRATED, SIMPLE }
}
