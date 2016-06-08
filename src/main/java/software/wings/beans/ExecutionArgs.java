/**
 *
 */

package software.wings.beans;

import java.util.List;

/**
 * The type Execution args.
 *
 * @author Rishi
 */
public class ExecutionArgs {
  private OrchestrationType orchestrationType;
  private String serviceId;
  private String commandName;
  private ExecutionStrategy executionStrategy;
  private String releaseId;
  private List<String> artifactIds;
  private String orchestrationId;
  private List<String> serviceInstanceIds;
  private ExecutionCredential executionCredential;

  /**
   * Gets service id.
   *
   * @return the service id
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Sets service id.
   *
   * @param serviceId the service id
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Gets command name.
   *
   * @return the command name
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Sets command name.
   *
   * @param commandName the command name
   */
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  /**
   * Gets release id.
   *
   * @return the release id
   */
  public String getReleaseId() {
    return releaseId;
  }

  /**
   * Sets release id.
   *
   * @param releaseId the release id
   */
  public void setReleaseId(String releaseId) {
    this.releaseId = releaseId;
  }

  /**
   * Gets artifact ids.
   *
   * @return the artifact ids
   */
  public List<String> getArtifactIds() {
    return artifactIds;
  }

  /**
   * Sets artifact ids.
   *
   * @param artifactIds the artifact ids
   */
  public void setArtifactIds(List<String> artifactIds) {
    this.artifactIds = artifactIds;
  }

  /**
   * Gets orchestration type.
   *
   * @return the orchestration type
   */
  public OrchestrationType getOrchestrationType() {
    return orchestrationType;
  }

  /**
   * Sets orchestration type.
   *
   * @param orchestrationType the orchestration type
   */
  public void setOrchestrationType(OrchestrationType orchestrationType) {
    this.orchestrationType = orchestrationType;
  }

  /**
   * Gets orchestration id.
   *
   * @return the orchestration id
   */
  public String getOrchestrationId() {
    return orchestrationId;
  }

  /**
   * Sets orchestration id.
   *
   * @param orchestrationId the orchestration id
   */
  public void setOrchestrationId(String orchestrationId) {
    this.orchestrationId = orchestrationId;
  }

  /**
   * Gets execution credential.
   *
   * @return the execution credential
   */
  public ExecutionCredential getExecutionCredential() {
    return executionCredential;
  }

  /**
   * Sets execution credential.
   *
   * @param executionCredential the execution credential
   */
  public void setExecutionCredential(ExecutionCredential executionCredential) {
    this.executionCredential = executionCredential;
  }

  /**
   * Gets execution strategy.
   *
   * @return the execution strategy
   */
  public ExecutionStrategy getExecutionStrategy() {
    return executionStrategy;
  }

  /**
   * Sets execution strategy.
   *
   * @param executionStrategy the execution strategy
   */
  public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
    this.executionStrategy = executionStrategy;
  }

  /**
   * Gets service instance ids.
   *
   * @return the service instance ids
   */
  public List<String> getServiceInstanceIds() {
    return serviceInstanceIds;
  }

  /**
   * Sets service instance ids.
   *
   * @param serviceInstanceIds the service instance ids
   */
  public void setServiceInstanceIds(List<String> serviceInstanceIds) {
    this.serviceInstanceIds = serviceInstanceIds;
  }

  /**
   * The enum Orchestration type.
   */
  public enum OrchestrationType {
    /**
     * Orchestrated orchestration type.
     */
    ORCHESTRATED, /**
                   * Simple orchestration type.
                   */
    SIMPLE
  }
}
