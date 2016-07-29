/**
 *
 */

package software.wings.beans;

import org.mongodb.morphia.annotations.Transient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The type Execution args.
 *
 * @author Rishi
 */
public class ExecutionArgs {
  private WorkflowType workflowType;
  private String serviceId;
  private String commandName;
  private ExecutionStrategy executionStrategy;
  private String releaseId;
  @Transient private List<Artifact> artifacts;
  private List<String> artifactIds;
  private String orchestrationId;
  @Transient private List<ServiceInstance> serviceInstances;
  private List<String> serviceInstanceIds;
  @Transient private ExecutionCredential executionCredential;

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
   * Gets artifacts.
   *
   * @return the artifacts
   */
  public List<Artifact> getArtifacts() {
    return artifacts;
  }

  /**
   * Sets artifacts.
   *
   * @param artifacts the artifacts
   */
  public void setArtifacts(List<Artifact> artifacts) {
    this.artifacts = artifacts;
  }

  /**
   * Gets workflow type.
   *
   * @return the workflow type
   */
  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  /**
   * Sets workflow type.
   *
   * @param workflowType the workflow type
   */
  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
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
   * Gets service instances.
   *
   * @return the service instances
   */
  public List<ServiceInstance> getServiceInstances() {
    return serviceInstances;
  }

  /**
   * Sets service instances.
   *
   * @param serviceInstances the service instances
   */
  public void setServiceInstances(List<ServiceInstance> serviceInstances) {
    this.serviceInstances = serviceInstances;
  }

  public List<String> getArtifactIds() {
    return artifactIds;
  }

  public void setArtifactIds(List<String> artifactIds) {
    this.artifactIds = artifactIds;
  }

  public List<String> getServiceInstanceIds() {
    return serviceInstanceIds;
  }

  public void setServiceInstanceIds(List<String> serviceInstanceIds) {
    this.serviceInstanceIds = serviceInstanceIds;
  }

  public void assignIds() {
    if (serviceInstances == null) {
      serviceInstanceIds = null;
    } else {
      serviceInstanceIds = serviceInstances.stream().map(ServiceInstance::getUuid).collect(Collectors.toList());
    }

    if (artifacts == null) {
      artifactIds = null;
    } else {
      artifactIds = artifacts.stream().map(Artifact::getUuid).collect(Collectors.toList());
    }
  }
}
