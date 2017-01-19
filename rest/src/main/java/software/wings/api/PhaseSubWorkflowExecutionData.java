package software.wings.api;

import software.wings.beans.DeploymentType;
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

  private String deploymentMasterId;

  private List<String> instanceIds;

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getComputeProviderId() {
    return computeProviderId;
  }

  public void setComputeProviderId(String computeProviderId) {
    this.computeProviderId = computeProviderId;
  }

  public DeploymentType getDeploymentType() {
    return deploymentType;
  }

  public void setDeploymentType(DeploymentType deploymentType) {
    this.deploymentType = deploymentType;
  }

  public String getDeploymentMasterId() {
    return deploymentMasterId;
  }

  public void setDeploymentMasterId(String deploymentMasterId) {
    this.deploymentMasterId = deploymentMasterId;
  }

  public List<String> getInstanceIds() {
    return instanceIds;
  }

  public void setInstanceIds(List<String> instanceIds) {
    this.instanceIds = instanceIds;
  }

  public static final class PhaseSubWorkflowExecutionDataBuilder {
    private String serviceId;
    private String computeProviderId;
    private DeploymentType deploymentType;
    private String stateName;
    private String deploymentMasterId;
    private Long startTs;
    private Long endTs;
    private List<String> instanceIds;
    private ExecutionStatus status;
    private String errorMsg;

    private PhaseSubWorkflowExecutionDataBuilder() {}

    public static PhaseSubWorkflowExecutionDataBuilder aPhaseSubWorkflowExecutionData() {
      return new PhaseSubWorkflowExecutionDataBuilder();
    }

    public PhaseSubWorkflowExecutionDataBuilder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public PhaseSubWorkflowExecutionDataBuilder withComputeProviderId(String computeProviderId) {
      this.computeProviderId = computeProviderId;
      return this;
    }

    public PhaseSubWorkflowExecutionDataBuilder withDeploymentType(DeploymentType deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public PhaseSubWorkflowExecutionDataBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public PhaseSubWorkflowExecutionDataBuilder withDeploymentMasterId(String deploymentMasterId) {
      this.deploymentMasterId = deploymentMasterId;
      return this;
    }

    public PhaseSubWorkflowExecutionDataBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public PhaseSubWorkflowExecutionDataBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public PhaseSubWorkflowExecutionDataBuilder withInstanceIds(List<String> instanceIds) {
      this.instanceIds = instanceIds;
      return this;
    }

    public PhaseSubWorkflowExecutionDataBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public PhaseSubWorkflowExecutionDataBuilder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public PhaseSubWorkflowExecutionData build() {
      PhaseSubWorkflowExecutionData phaseSubWorkflowExecutionData = new PhaseSubWorkflowExecutionData();
      phaseSubWorkflowExecutionData.setServiceId(serviceId);
      phaseSubWorkflowExecutionData.setComputeProviderId(computeProviderId);
      phaseSubWorkflowExecutionData.setDeploymentType(deploymentType);
      phaseSubWorkflowExecutionData.setStateName(stateName);
      phaseSubWorkflowExecutionData.setDeploymentMasterId(deploymentMasterId);
      phaseSubWorkflowExecutionData.setStartTs(startTs);
      phaseSubWorkflowExecutionData.setEndTs(endTs);
      phaseSubWorkflowExecutionData.setInstanceIds(instanceIds);
      phaseSubWorkflowExecutionData.setStatus(status);
      phaseSubWorkflowExecutionData.setErrorMsg(errorMsg);
      return phaseSubWorkflowExecutionData;
    }
  }
}