package software.wings.service.impl.appdynamics;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 5/15/17.
 */
@Data
public class AppdynamicsNode implements Comparable<AppdynamicsNode> {
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

  @Override
  public int compareTo(AppdynamicsNode o) {
    return name.compareTo(o.getName());
  }
}
