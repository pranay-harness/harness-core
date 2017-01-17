package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.List;

/**
 * Created by rishi on 1/12/17.
 */
public class DcNodeSelectState extends State {
  public DcNodeSelectState(String name) {
    super(name, StateType.DC_NODE_SELECT.name());
  }

  private String serviceId;
  private String computeProviderId;
  private boolean random;
  private int randomInstanceCount;
  private List<String> instanceIds;
  private List<String> excludeInstanceIds;

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    // It should return list of InstanceIds
    return new ExecutionResponse();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

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

  public boolean isRandom() {
    return random;
  }

  public void setRandom(boolean random) {
    this.random = random;
  }

  public int getRandomInstanceCount() {
    return randomInstanceCount;
  }

  public void setRandomInstanceCount(int randomInstanceCount) {
    this.randomInstanceCount = randomInstanceCount;
  }

  public List<String> getInstanceIds() {
    return instanceIds;
  }

  public void setInstanceIds(List<String> instanceIds) {
    this.instanceIds = instanceIds;
  }

  public List<String> getExcludeInstanceIds() {
    return excludeInstanceIds;
  }

  public void setExcludeInstanceIds(List<String> excludeInstanceIds) {
    this.excludeInstanceIds = excludeInstanceIds;
  }
}
