/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutput;

import software.wings.beans.ElementExecutionSummary;
import software.wings.sm.ContextElement;
import software.wings.sm.states.ElementStateExecutionData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 1/19/17.
 */
@OwnedBy(CDC)
@JsonTypeName("phaseExecutionData")
@TargetModule(HarnessModule._957_CG_BEANS)
public class PhaseExecutionData extends ElementStateExecutionData implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "phaseExecutionData";

  private String serviceId;
  private String serviceName;
  private String computeProviderId;
  private String computeProviderName;
  private String computeProviderType;
  private String deploymentType;
  private String infraMappingId;
  private String infraDefinitionId;
  private String infraMappingName;
  private String clusterName;
  private String containerServiceName;
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

  public String getDeploymentType() {
    return deploymentType;
  }

  public void setDeploymentType(String deploymentType) {
    this.deploymentType = deploymentType;
  }

  public String getInfraMappingId() {
    return infraMappingId;
  }

  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  public String getInfraDefinitionId() {
    return infraDefinitionId;
  }

  public void setInfraDefinitionId(String infraDefinitionId) {
    this.infraDefinitionId = infraDefinitionId;
  }

  public List<String> getInstanceIds() {
    return instanceIds;
  }

  public void setInstanceIds(List<String> instanceIds) {
    this.instanceIds = instanceIds;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getComputeProviderName() {
    return computeProviderName;
  }

  public void setComputeProviderName(String computeProviderName) {
    this.computeProviderName = computeProviderName;
  }

  public String getComputeProviderType() {
    return computeProviderType;
  }

  public void setComputeProviderType(String computeProviderType) {
    this.computeProviderType = computeProviderType;
  }

  public String getInfraMappingName() {
    return infraMappingName;
  }

  public void setInfraMappingName(String infraMappingName) {
    this.infraMappingName = infraMappingName;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getContainerServiceName() {
    return containerServiceName;
  }

  public void setContainerServiceName(String containerServiceName) {
    this.containerServiceName = containerServiceName;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "serviceName",
        ExecutionDataValue.builder().displayName("Service Name").value(serviceName).build());
    putNotNull(executionDetails, "deploymentType",
        ExecutionDataValue.builder().displayName("Deployment Type").value(deploymentType).build());
    putNotNull(executionDetails, "computeProviderType",
        ExecutionDataValue.builder().displayName("Cloud Provider Type").value(computeProviderType).build());
    putNotNull(executionDetails, "computeProviderName",
        ExecutionDataValue.builder().displayName("Cloud Provider").value(computeProviderName).build());
    putNotNull(executionDetails, "clusterName",
        ExecutionDataValue.builder().displayName("Cluster Name").value(clusterName).build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "serviceName",
        ExecutionDataValue.builder().displayName("Service Name").value(serviceName).build());
    putNotNull(executionDetails, "deploymentType",
        ExecutionDataValue.builder().displayName("Deployment Type").value(deploymentType).build());
    putNotNull(executionDetails, "computeProviderType",
        ExecutionDataValue.builder().displayName("Cloud Provider Type").value(computeProviderType).build());
    putNotNull(executionDetails, "computeProviderName",
        ExecutionDataValue.builder().displayName("Cloud Provider").value(computeProviderName).build());
    putNotNull(executionDetails, "clusterName",
        ExecutionDataValue.builder().displayName("Cluster Name").value(clusterName).build());
    return executionDetails;
  }

  @Override
  public String getType() {
    return "phaseExecutionData";
  }

  public static final class PhaseExecutionDataBuilder {
    private String serviceId;
    private String serviceName;
    private String computeProviderId;
    private String computeProviderName;
    private String computeProviderType;
    private List<ElementExecutionSummary> elementStatusSummary = new ArrayList<>();
    private String deploymentType;
    private String stateName;
    private String infraMappingId;
    private String infraDefinitionId;
    private Map<String, ExecutionStatus> instanceIdStatusMap = new HashMap<>();
    private Long startTs;
    private String infraMappingName;
    private Long endTs;
    private String clusterName;
    private String containerServiceName;
    private ExecutionStatus status;
    private List<String> instanceIds;
    private String errorMsg;
    private Integer waitInterval;
    private ContextElement element;

    private PhaseExecutionDataBuilder() {}

    public static PhaseExecutionDataBuilder aPhaseExecutionData() {
      return new PhaseExecutionDataBuilder();
    }

    public PhaseExecutionDataBuilder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public PhaseExecutionDataBuilder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public PhaseExecutionDataBuilder withComputeProviderId(String computeProviderId) {
      this.computeProviderId = computeProviderId;
      return this;
    }

    public PhaseExecutionDataBuilder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public PhaseExecutionDataBuilder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    public PhaseExecutionDataBuilder withElementStatusSummary(List<ElementExecutionSummary> elementStatusSummary) {
      this.elementStatusSummary = elementStatusSummary;
      return this;
    }

    public PhaseExecutionDataBuilder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public PhaseExecutionDataBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public PhaseExecutionDataBuilder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public PhaseExecutionDataBuilder withInfraDefinitionId(String infraDefinitionId) {
      this.infraDefinitionId = infraDefinitionId;
      return this;
    }

    public PhaseExecutionDataBuilder withInstanceIdStatusMap(Map<String, ExecutionStatus> instanceIdStatusMap) {
      this.instanceIdStatusMap = instanceIdStatusMap;
      return this;
    }

    public PhaseExecutionDataBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public PhaseExecutionDataBuilder withInfraMappingName(String infraMappingName) {
      this.infraMappingName = infraMappingName;
      return this;
    }

    public PhaseExecutionDataBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public PhaseExecutionDataBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public PhaseExecutionDataBuilder withContainerServiceName(String containerServiceName) {
      this.containerServiceName = containerServiceName;
      return this;
    }

    public PhaseExecutionDataBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public PhaseExecutionDataBuilder withInstanceIds(List<String> instanceIds) {
      this.instanceIds = instanceIds;
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

    public PhaseExecutionDataBuilder withElement(ContextElement element) {
      this.element = element;
      return this;
    }

    public PhaseExecutionData build() {
      PhaseExecutionData phaseExecutionData = new PhaseExecutionData();
      phaseExecutionData.setServiceId(serviceId);
      phaseExecutionData.setServiceName(serviceName);
      phaseExecutionData.setComputeProviderId(computeProviderId);
      phaseExecutionData.setComputeProviderName(computeProviderName);
      phaseExecutionData.setComputeProviderType(computeProviderType);
      phaseExecutionData.setElementStatusSummary(elementStatusSummary);
      phaseExecutionData.setDeploymentType(deploymentType);
      phaseExecutionData.setStateName(stateName);
      phaseExecutionData.setInfraMappingId(infraMappingId);
      phaseExecutionData.setInstanceIdStatusMap(instanceIdStatusMap);
      phaseExecutionData.setStartTs(startTs);
      phaseExecutionData.setInfraMappingName(infraMappingName);
      phaseExecutionData.setEndTs(endTs);
      phaseExecutionData.setClusterName(clusterName);
      phaseExecutionData.setContainerServiceName(containerServiceName);
      phaseExecutionData.setStatus(status);
      phaseExecutionData.setInstanceIds(instanceIds);
      phaseExecutionData.setErrorMsg(errorMsg);
      phaseExecutionData.setWaitInterval(waitInterval);
      phaseExecutionData.setElement(element);
      phaseExecutionData.setInfraDefinitionId(infraDefinitionId);
      return phaseExecutionData;
    }
  }
}
