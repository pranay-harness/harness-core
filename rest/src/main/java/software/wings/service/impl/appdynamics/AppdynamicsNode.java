package software.wings.service.impl.appdynamics;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 5/15/17.
 */
public class AppdynamicsNode {
  private long id;
  private String name;
  private String type;
  private long tierId;
  private String tierName;
  private long machineId;
  private String machineName;
  private String machineOSType;
  private boolean machineAgentPresent;
  private boolean appAgentPresent;
  private String appAgentVersion;
  private String agentType;
  private Map<String, List<String>> ipAddresses;

  // backward compatibilty
  private String machineAgentVersion;
  private String nodeUniqueLocalId;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public long getTierId() {
    return tierId;
  }

  public void setTierId(long tierId) {
    this.tierId = tierId;
  }

  public String getTierName() {
    return tierName;
  }

  public void setTierName(String tierName) {
    this.tierName = tierName;
  }

  public long getMachineId() {
    return machineId;
  }

  public void setMachineId(long machineId) {
    this.machineId = machineId;
  }

  public String getMachineName() {
    return machineName;
  }

  public void setMachineName(String machineName) {
    this.machineName = machineName;
  }

  public String getMachineOSType() {
    return machineOSType;
  }

  public void setMachineOSType(String machineOSType) {
    this.machineOSType = machineOSType;
  }

  public boolean isMachineAgentPresent() {
    return machineAgentPresent;
  }

  public void setMachineAgentPresent(boolean machineAgentPresent) {
    this.machineAgentPresent = machineAgentPresent;
  }

  public boolean isAppAgentPresent() {
    return appAgentPresent;
  }

  public void setAppAgentPresent(boolean appAgentPresent) {
    this.appAgentPresent = appAgentPresent;
  }

  public String getAppAgentVersion() {
    return appAgentVersion;
  }

  public void setAppAgentVersion(String appAgentVersion) {
    this.appAgentVersion = appAgentVersion;
  }

  public String getAgentType() {
    return agentType;
  }

  public void setAgentType(String agentType) {
    this.agentType = agentType;
  }

  public String getMachineAgentVersion() {
    return machineAgentVersion;
  }

  public void setMachineAgentVersion(String machineAgentVersion) {
    this.machineAgentVersion = machineAgentVersion;
  }

  public String getNodeUniqueLocalId() {
    return nodeUniqueLocalId;
  }

  public void setNodeUniqueLocalId(String nodeUniqueLocalId) {
    this.nodeUniqueLocalId = nodeUniqueLocalId;
  }

  public Map<String, List<String>> getIpAddresses() {
    return ipAddresses;
  }

  public void setIpAddresses(Map<String, List<String>> ipAddresses) {
    this.ipAddresses = ipAddresses;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    AppdynamicsNode that = (AppdynamicsNode) o;

    return id == that.id;
  }

  @Override
  public int hashCode() {
    return (int) (id ^ (id >>> 32));
  }

  @Override
  public String toString() {
    return "AppdynamicsNode{"
        + "id=" + id + ", name='" + name + '\'' + ", type='" + type + '\'' + ", tierId=" + tierId + ", tierName='"
        + tierName + '\'' + ", machineId=" + machineId + ", machineName='" + machineName + '\'' + ", machineOSType='"
        + machineOSType + '\'' + ", machineAgentPresent=" + machineAgentPresent + ", appAgentPresent=" + appAgentPresent
        + ", appAgentVersion='" + appAgentVersion + '\'' + ", agentType='" + agentType + '\''
        + ", ipAddresses=" + ipAddresses + ", machineAgentVersion='" + machineAgentVersion + '\''
        + ", nodeUniqueLocalId='" + nodeUniqueLocalId + '\'' + '}';
  }
}
