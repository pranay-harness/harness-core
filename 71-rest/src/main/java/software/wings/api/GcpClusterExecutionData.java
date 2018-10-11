package software.wings.api;

import io.harness.task.protocol.ResponseData;
import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * Created by brett on 4/13/17
 */
public class GcpClusterExecutionData extends StateExecutionData implements ResponseData {
  private String clusterName;
  private String zone;
  private int nodeCount;
  private String machineType;

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getZone() {
    return zone;
  }

  public void setZone(String zone) {
    this.zone = zone;
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
    putNotNull(executionDetails, "zone", ExecutionDataValue.builder().displayName("Zone").value(zone).build());
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
    putNotNull(executionDetails, "zone", ExecutionDataValue.builder().displayName("Zone").value(zone).build());
    putNotNull(
        executionDetails, "nodeCount", ExecutionDataValue.builder().displayName("Node Count").value(nodeCount).build());
    putNotNull(executionDetails, "machineType",
        ExecutionDataValue.builder().displayName("Machine Type").value(machineType).build());
    return executionDetails;
  }

  public static final class GcpClusterExecutionDataBuilder {
    private String clusterName;
    private String zone;
    private int nodeCount;
    private String machineType;

    private GcpClusterExecutionDataBuilder() {}

    public static GcpClusterExecutionDataBuilder aGcpClusterExecutionData() {
      return new GcpClusterExecutionDataBuilder();
    }

    public GcpClusterExecutionDataBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public GcpClusterExecutionDataBuilder withZone(String zone) {
      this.zone = zone;
      return this;
    }

    public GcpClusterExecutionDataBuilder withNodeCount(int nodeCount) {
      this.nodeCount = nodeCount;
      return this;
    }

    public GcpClusterExecutionDataBuilder withMachineType(String machineType) {
      this.machineType = machineType;
      return this;
    }

    public GcpClusterExecutionDataBuilder but() {
      return aGcpClusterExecutionData()
          .withClusterName(clusterName)
          .withZone(zone)
          .withNodeCount(nodeCount)
          .withMachineType(machineType);
    }

    public GcpClusterExecutionData build() {
      GcpClusterExecutionData gcpClusterExecutionData = new GcpClusterExecutionData();
      gcpClusterExecutionData.setClusterName(clusterName);
      gcpClusterExecutionData.setZone(zone);
      gcpClusterExecutionData.setNodeCount(nodeCount);
      gcpClusterExecutionData.setMachineType(machineType);
      return gcpClusterExecutionData;
    }
  }
}
