/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * Created by brett on 4/13/17
 */
@OwnedBy(CDP)
public class AwsClusterExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String clusterName;
  private String region;
  private int nodeCount;
  private String machineType;

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public int getNodeCount() {
    return nodeCount;
  }

  public void setNodeCount(int nodeCount) {
    this.nodeCount = nodeCount;
  }

  public String getMachineType() {
    return machineType;
  }

  public void setMachineType(String machineType) {
    this.machineType = machineType;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "clusterName",
        ExecutionDataValue.builder().displayName("Cluster Name").value(clusterName).build());
    putNotNull(executionDetails, "region", ExecutionDataValue.builder().displayName("Zone").value(region).build());
    putNotNull(
        executionDetails, "nodeCount", ExecutionDataValue.builder().displayName("Node Count").value(nodeCount).build());
    putNotNull(executionDetails, "machineType",
        ExecutionDataValue.builder().displayName("Machine Type").value(machineType).build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "clusterName",
        ExecutionDataValue.builder().displayName("Cluster Name").value(clusterName).build());
    putNotNull(executionDetails, "region", ExecutionDataValue.builder().displayName("Zone").value(region).build());
    putNotNull(
        executionDetails, "nodeCount", ExecutionDataValue.builder().displayName("Node Count").value(nodeCount).build());
    putNotNull(executionDetails, "machineType",
        ExecutionDataValue.builder().displayName("Machine Type").value(machineType).build());
    return executionDetails;
  }

  public static final class AwsClusterExecutionDataBuilder {
    private String clusterName;
    private String region;
    private int nodeCount;
    private String machineType;

    private AwsClusterExecutionDataBuilder() {}

    public static AwsClusterExecutionDataBuilder anAwsClusterExecutionData() {
      return new AwsClusterExecutionDataBuilder();
    }

    public AwsClusterExecutionDataBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public AwsClusterExecutionDataBuilder withRegion(String region) {
      this.region = region;
      return this;
    }

    public AwsClusterExecutionDataBuilder withNodeCount(int nodeCount) {
      this.nodeCount = nodeCount;
      return this;
    }

    public AwsClusterExecutionDataBuilder withMachineType(String machineType) {
      this.machineType = machineType;
      return this;
    }

    public AwsClusterExecutionDataBuilder but() {
      return anAwsClusterExecutionData()
          .withClusterName(clusterName)
          .withRegion(region)
          .withNodeCount(nodeCount)
          .withMachineType(machineType);
    }

    public AwsClusterExecutionData build() {
      AwsClusterExecutionData awsClusterExecutionData = new AwsClusterExecutionData();
      awsClusterExecutionData.setClusterName(clusterName);
      awsClusterExecutionData.setRegion(region);
      awsClusterExecutionData.setNodeCount(nodeCount);
      awsClusterExecutionData.setMachineType(machineType);
      return awsClusterExecutionData;
    }
  }
}
