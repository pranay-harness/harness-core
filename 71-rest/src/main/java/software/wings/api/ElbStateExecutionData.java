package software.wings.api;

import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 9/12/16.
 */
public class ElbStateExecutionData extends StateExecutionData {
  private String hostName;

  @java.beans.ConstructorProperties({"hostName"})
  ElbStateExecutionData(String hostName) {
    this.hostName = hostName;
  }

  public static ElbStateExecutionDataBuilder builder() {
    return new ElbStateExecutionDataBuilder();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> data = super.getExecutionSummary();
    putNotNull(data, "hostName", ExecutionDataValue.builder().displayName("Host").value(hostName).build());
    return data;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> data = super.getExecutionDetails();
    putNotNull(data, "hostName", ExecutionDataValue.builder().displayName("Host").value(hostName).build());
    return data;
  }

  public String getHostName() {
    return this.hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ElbStateExecutionData)) {
      return false;
    }
    final ElbStateExecutionData other = (ElbStateExecutionData) o;
    if (!other.canEqual((Object) this)) {
      return false;
    }
    final Object this$hostName = this.getHostName();
    final Object other$hostName = other.getHostName();
    if (this$hostName == null ? other$hostName != null : !this$hostName.equals(other$hostName)) {
      return false;
    }
    return true;
  }

  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final Object $hostName = this.getHostName();
    result = result * PRIME + ($hostName == null ? 43 : $hostName.hashCode());
    return result;
  }

  protected boolean canEqual(Object other) {
    return other instanceof ElbStateExecutionData;
  }

  public String toString() {
    return "software.wings.api.ElbStateExecutionData(hostName=" + this.getHostName() + ")";
  }

  public static class ElbStateExecutionDataBuilder {
    private String hostName;

    ElbStateExecutionDataBuilder() {}

    public ElbStateExecutionDataBuilder hostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public ElbStateExecutionData build() {
      return new ElbStateExecutionData(hostName);
    }

    public String toString() {
      return "software.wings.api.ElbStateExecutionData.ElbStateExecutionDataBuilder(hostName=" + this.hostName + ")";
    }
  }
}
