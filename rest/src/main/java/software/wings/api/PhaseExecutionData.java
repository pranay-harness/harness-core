package software.wings.api;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.states.ElementStateExecutionData;

import java.util.List;

/**
 * Created by rishi on 1/19/17.
 */
public class PhaseExecutionData extends ElementStateExecutionData {
  private String serviceId;
  private String computeProviderId;
  private DeploymentType deploymentType;
  private String infraMappingId;
  private List<String> instanceIds;

  private PhaseExecutionSummary phaseExecutionSummary;

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

  public String getInfraMappingId() {
    return infraMappingId;
  }

  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  public List<String> getInstanceIds() {
    return instanceIds;
  }

  public void setInstanceIds(List<String> instanceIds) {
    this.instanceIds = instanceIds;
  }

  public PhaseExecutionSummary getPhaseExecutionSummary() {
    return phaseExecutionSummary;
  }

  public void setPhaseExecutionSummary(PhaseExecutionSummary phaseExecutionSummary) {
    this.phaseExecutionSummary = phaseExecutionSummary;
  }

  public static final class PhaseExecutionDataBuilder {
    private String serviceId;
    private String computeProviderId;
    private DeploymentType deploymentType;
    private String infraMappingId;
    private List<String> instanceIds;
    private String stateName;
    private PhaseExecutionSummary phaseExecutionSummary;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;
    private Integer waitInterval;

    private PhaseExecutionDataBuilder() {}

    public static PhaseExecutionDataBuilder aPhaseExecutionData() {
      return new PhaseExecutionDataBuilder();
    }

    public PhaseExecutionDataBuilder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public PhaseExecutionDataBuilder withComputeProviderId(String computeProviderId) {
      this.computeProviderId = computeProviderId;
      return this;
    }

    public PhaseExecutionDataBuilder withDeploymentType(DeploymentType deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public PhaseExecutionDataBuilder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public PhaseExecutionDataBuilder withInstanceIds(List<String> instanceIds) {
      this.instanceIds = instanceIds;
      return this;
    }

    public PhaseExecutionDataBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public PhaseExecutionDataBuilder withPhaseExecutionSummary(PhaseExecutionSummary phaseExecutionSummary) {
      this.phaseExecutionSummary = phaseExecutionSummary;
      return this;
    }

    public PhaseExecutionDataBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public PhaseExecutionDataBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public PhaseExecutionDataBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public PhaseExecutionDataBuilder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public PhaseExecutionDataBuilder withWaitInterval(Integer waitInterval) {
      this.waitInterval = waitInterval;
      return this;
    }

    public PhaseExecutionData build() {
      PhaseExecutionData phaseExecutionData = new PhaseExecutionData();
      phaseExecutionData.setServiceId(serviceId);
      phaseExecutionData.setComputeProviderId(computeProviderId);
      phaseExecutionData.setDeploymentType(deploymentType);
      phaseExecutionData.setInfraMappingId(infraMappingId);
      phaseExecutionData.setInstanceIds(instanceIds);
      phaseExecutionData.setStateName(stateName);
      phaseExecutionData.setPhaseExecutionSummary(phaseExecutionSummary);
      phaseExecutionData.setStartTs(startTs);
      phaseExecutionData.setEndTs(endTs);
      phaseExecutionData.setStatus(status);
      phaseExecutionData.setErrorMsg(errorMsg);
      phaseExecutionData.setWaitInterval(waitInterval);
      return phaseExecutionData;
    }
  }
}
