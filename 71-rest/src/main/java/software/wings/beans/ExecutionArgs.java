/**
 *
 */

package software.wings.beans;

import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.artifact.Artifact;

import java.util.List;
import java.util.Map;

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
  private Map<String, String> workflowVariables;
  private String notes;
  private EmbeddedUser triggeredBy;
  private boolean excludeHostsWithSameArtifact;

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

  /**
   * Gets artifact id names.
   *
   * @return the artifact id names
   */
  public Map<String, String> getArtifactIdNames() {
    return artifactIdNames;
  }

  /**
   * Sets artifact id names.
   *
   * @param artifactIdNames the artifact id names
   */
  public void setArtifactIdNames(Map<String, String> artifactIdNames) {
    this.artifactIdNames = artifactIdNames;
  }

  /**
   * Gets service instance id names.
   *
   * @return the service instance id names
   */
  public Map<String, String> getServiceInstanceIdNames() {
    return serviceInstanceIdNames;
  }

  /**
   * Sets service instance id names.
   *
   * @param serviceInstanceIdNames the service instance id names
   */
  public void setServiceInstanceIdNames(Map<String, String> serviceInstanceIdNames) {
    this.serviceInstanceIdNames = serviceInstanceIdNames;
  }

  /**
   * Gets error strategy.
   *
   * @return the error strategy
   */
  public ErrorStrategy getErrorStrategy() {
    return errorStrategy;
  }

  /**
   * Sets error strategy.
   *
   * @param errorStrategy the error strategy
   */
  public void setErrorStrategy(ErrorStrategy errorStrategy) {
    this.errorStrategy = errorStrategy;
  }

  public boolean isTriggeredFromPipeline() {
    return triggeredFromPipeline;
  }

  public void setTriggeredFromPipeline(boolean triggeredFromPipeline) {
    this.triggeredFromPipeline = triggeredFromPipeline;
  }

  public String getPipelineId() {
    return pipelineId;
  }

  public void setPipelineId(String pipelineId) {
    this.pipelineId = pipelineId;
  }

  public String getPipelinePhaseElementId() {
    return pipelinePhaseElementId;
  }

  public void setPipelinePhaseElementId(String pipelinePhaseElementId) {
    this.pipelinePhaseElementId = pipelinePhaseElementId;
  }

  public Map<String, String> getWorkflowVariables() {
    return workflowVariables;
  }

  public void setWorkflowVariables(Map<String, String> workflowVariables) {
    this.workflowVariables = workflowVariables;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  /**
   * Returns triggered by
   * @return
   */
  public EmbeddedUser getTriggeredBy() {
    return triggeredBy;
  }

  public void setTriggeredBy(EmbeddedUser triggeredBy) {
    this.triggeredBy = triggeredBy;
  }

  /**
   * It excludes hosts that have already same artifact installed
   * @return
   */
  public boolean isExcludeHostsWithSameArtifact() {
    return excludeHostsWithSameArtifact;
  }

  public void setExcludeHostsWithSameArtifact(boolean excludeHostsWithSameArtifact) {
    this.excludeHostsWithSameArtifact = excludeHostsWithSameArtifact;
  }
}
