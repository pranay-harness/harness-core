package software.wings.sm.states;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ServiceInstance;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by rishi on 1/12/17.
 */
public class DcNodeSelectState extends State {
  private static final Logger logger = LoggerFactory.getLogger(DcNodeSelectState.class);

  private String serviceId;
  private String computeProviderId;
  private boolean random;
  private int randomInstanceCount;
  private List<String> instanceIds;
  private List<String> excludeInstanceIds;

  @Inject private InfrastructureMappingService infrastructureMappingService;

  public DcNodeSelectState(String name) {
    super(name, StateType.DC_NODE_SELECT.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String appId = ((ExecutionContextImpl) context).getApp().getUuid();
    String envId = ((ExecutionContextImpl) context).getEnv().getUuid();

    logger.info("serviceId : {}, computeProviderId: {}", serviceId, computeProviderId);

    List<ServiceInstance> serviceInstances =
        infrastructureMappingService.selectServiceInstances(appId, serviceId, envId, computeProviderId,
            ImmutableMap.of("random", random, "randomInstanceCount", randomInstanceCount, "instanceIds", instanceIds,
                "excludeInstanceIds", excludeInstanceIds));

    List<String> serviceInstancesIds = serviceInstances.stream().map(ServiceInstance::getUuid).collect(toList());
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
