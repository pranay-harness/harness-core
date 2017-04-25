package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by brett on 4/13/17
 */
public class AwsClusterExecutionData extends StateExecutionData implements NotifyResponseData {
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
        anExecutionDataValue().withValue(clusterName).withDisplayName("Cluster Name").build());
    putNotNull(executionDetails, "region", anExecutionDataValue().withValue(region).withDisplayName("Zone").build());
    putNotNull(executionDetails, "nodeCount",
        anExecutionDataValue().withValue(nodeCount).withDisplayName("Node Count").build());
    putNotNull(executionDetails, "machineType",
        anExecutionDataValue().withValue(machineType).withDisplayName("Machine Type").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "clusterName",
        anExecutionDataValue().withValue(clusterName).withDisplayName("Cluster Name").build());
    putNotNull(executionDetails, "region", anExecutionDataValue().withValue(region).withDisplayName("Zone").build());
    putNotNull(executionDetails, "nodeCount",
        anExecutionDataValue().withValue(nodeCount).withDisplayName("Node Count").build());
    putNotNull(executionDetails, "machineType",
        anExecutionDataValue().withValue(machineType).withDisplayName("Machine Type").build());
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
