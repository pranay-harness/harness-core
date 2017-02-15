package software.wings.api;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.ElementStateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;

/**
 * Created by rishi on 1/19/17.
 */
public class PhaseSubWorkflowExecutionData extends ElementStateExecutionData implements NotifyResponseData {
  private String serviceId;
  private String computeProviderId;
  private DeploymentType deploymentType;
  private String infraMappingId;

  private List<String> instanceIds;

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
   * Gets compute provider id.
   *
   * @return the compute provider id
   */
  public String getComputeProviderId() {
    return computeProviderId;
  }

  /**
   * Sets compute provider id.
   *
   * @param computeProviderId the compute provider id
   */
  public void setComputeProviderId(String computeProviderId) {
    this.computeProviderId = computeProviderId;
  }

  /**
   * Gets deployment type.
   *
   * @return the deployment type
   */
  public DeploymentType getDeploymentType() {
    return deploymentType;
  }

  /**
   * Sets deployment type.
   *
   * @param deploymentType the deployment type
   */
  public void setDeploymentType(DeploymentType deploymentType) {
    this.deploymentType = deploymentType;
  }

  /**
   * Gets instance ids.
   *
   * @return the instance ids
   */
  public List<String> getInstanceIds() {
    return instanceIds;
  }

  /**
   * Sets instance ids.
   *
   * @param instanceIds the instance ids
   */
  public void setInstanceIds(List<String> instanceIds) {
    this.instanceIds = instanceIds;
  }

  /**
   * Gets infra mapping id.
   *
   * @return the infra mapping id
   */
  public String getInfraMappingId() {
    return infraMappingId;
  }

  /**
   * Sets infra mapping id.
   *
   * @param infraMappingId the infra mapping id
   */
  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  /**
   * The type Phase sub workflow execution data builder.
   */
  public static final class PhaseSubWorkflowExecutionDataBuilder {
    private String serviceId;
    private String computeProviderId;
    private DeploymentType deploymentType;
    private String infraMappingId;
    private String stateName;
    private Long startTs;
    private List<String> instanceIds;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;

    private PhaseSubWorkflowExecutionDataBuilder() {}

    /**
     * A phase sub workflow execution data phase sub workflow execution data builder.
     *
     * @return the phase sub workflow execution data builder
     */
    public static PhaseSubWorkflowExecutionDataBuilder aPhaseSubWorkflowExecutionData() {
      return new PhaseSubWorkflowExecutionDataBuilder();
    }

    /**
     * With service id phase sub workflow execution data builder.
     *
     * @param serviceId the service id
     * @return the phase sub workflow execution data builder
     */
    public PhaseSubWorkflowExecutionDataBuilder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * With compute provider id phase sub workflow execution data builder.
     *
     * @param computeProviderId the compute provider id
     * @return the phase sub workflow execution data builder
     */
    public PhaseSubWorkflowExecutionDataBuilder withComputeProviderId(String computeProviderId) {
      this.computeProviderId = computeProviderId;
      return this;
    }

    /**
     * With deployment type phase sub workflow execution data builder.
     *
     * @param deploymentType the deployment type
     * @return the phase sub workflow execution data builder
     */
    public PhaseSubWorkflowExecutionDataBuilder withDeploymentType(DeploymentType deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    /**
     * With infra mapping id phase sub workflow execution data builder.
     *
     * @param infraMappingId the infra mapping id
     * @return the phase sub workflow execution data builder
     */
    public PhaseSubWorkflowExecutionDataBuilder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    /**
     * With state name phase sub workflow execution data builder.
     *
     * @param stateName the state name
     * @return the phase sub workflow execution data builder
     */
    public PhaseSubWorkflowExecutionDataBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * With start ts phase sub workflow execution data builder.
     *
     * @param startTs the start ts
     * @return the phase sub workflow execution data builder
     */
    public PhaseSubWorkflowExecutionDataBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With instance ids phase sub workflow execution data builder.
     *
     * @param instanceIds the instance ids
     * @return the phase sub workflow execution data builder
     */
    public PhaseSubWorkflowExecutionDataBuilder withInstanceIds(List<String> instanceIds) {
      this.instanceIds = instanceIds;
      return this;
    }

    /**
     * With end ts phase sub workflow execution data builder.
     *
     * @param endTs the end ts
     * @return the phase sub workflow execution data builder
     */
    public PhaseSubWorkflowExecutionDataBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With status phase sub workflow execution data builder.
     *
     * @param status the status
     * @return the phase sub workflow execution data builder
     */
    public PhaseSubWorkflowExecutionDataBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With error msg phase sub workflow execution data builder.
     *
     * @param errorMsg the error msg
     * @return the phase sub workflow execution data builder
     */
    public PhaseSubWorkflowExecutionDataBuilder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    /**
     * But phase sub workflow execution data builder.
     *
     * @return the phase sub workflow execution data builder
     */
    public PhaseSubWorkflowExecutionDataBuilder but() {
      return aPhaseSubWorkflowExecutionData()
          .withServiceId(serviceId)
          .withComputeProviderId(computeProviderId)
          .withDeploymentType(deploymentType)
          .withInfraMappingId(infraMappingId)
          .withStateName(stateName)
          .withStartTs(startTs)
          .withInstanceIds(instanceIds)
          .withEndTs(endTs)
          .withStatus(status)
          .withErrorMsg(errorMsg);
    }

    /**
     * Build phase sub workflow execution data.
     *
     * @return the phase sub workflow execution data
     */
    public PhaseSubWorkflowExecutionData build() {
      PhaseSubWorkflowExecutionData phaseSubWorkflowExecutionData = new PhaseSubWorkflowExecutionData();
      phaseSubWorkflowExecutionData.setServiceId(serviceId);
      phaseSubWorkflowExecutionData.setComputeProviderId(computeProviderId);
      phaseSubWorkflowExecutionData.setDeploymentType(deploymentType);
      phaseSubWorkflowExecutionData.setInfraMappingId(infraMappingId);
      phaseSubWorkflowExecutionData.setStateName(stateName);
      phaseSubWorkflowExecutionData.setStartTs(startTs);
      phaseSubWorkflowExecutionData.setInstanceIds(instanceIds);
      phaseSubWorkflowExecutionData.setEndTs(endTs);
      phaseSubWorkflowExecutionData.setStatus(status);
      phaseSubWorkflowExecutionData.setErrorMsg(errorMsg);
      return phaseSubWorkflowExecutionData;
    }
  }
}
