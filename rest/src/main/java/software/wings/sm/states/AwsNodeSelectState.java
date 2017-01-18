package software.wings.sm.states;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableMap;

import com.github.reinert.jjschema.Attributes;
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
public class AwsNodeSelectState extends State {
  private static final Logger logger = LoggerFactory.getLogger(AwsNodeSelectState.class);

  private String serviceId;
  private String computeProviderId;
  @Attributes(title = "Number of instances") private int instanceCount;
  @Attributes(title = "Select specific hosts") private boolean specificHosts;
  private List<String> hostNames;

  @Inject private InfrastructureMappingService infrastructureMappingService;

  /**
   * Instantiates a new Aws node select state.
   *
   * @param name the name
   */
  public AwsNodeSelectState(String name) {
    super(name, StateType.AWS_NODE_SELECT.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String appId = ((ExecutionContextImpl) context).getApp().getUuid();
    String envId = ((ExecutionContextImpl) context).getEnv().getUuid();

    logger.info("serviceId : {}, computeProviderId: {}", serviceId, computeProviderId);

    List<ServiceInstance> serviceInstances;
    if (specificHosts) {
      serviceInstances = infrastructureMappingService.selectServiceInstances(appId, serviceId, envId, computeProviderId,
          ImmutableMap.of("specificHosts", specificHosts, "hostNames", hostNames));
    } else {
      serviceInstances = infrastructureMappingService.selectServiceInstances(appId, serviceId, envId, computeProviderId,
          ImmutableMap.of("specificHosts", specificHosts, "instanceCount", instanceCount));
    }
    List<String> serviceInstancesIds = serviceInstances.stream().map(ServiceInstance::getUuid).collect(toList());
    return new ExecutionResponse();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets service id.
   *
   * @return the service id
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Sets service id.
   *
   * @param serviceId the service id
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Gets compute provider id.
   *
   * @return the compute provider id
   */
  public String getComputeProviderId() {
    return computeProviderId;
  }

  /**
   * Sets compute provider id.
   *
   * @param computeProviderId the compute provider id
   */
  public void setComputeProviderId(String computeProviderId) {
    this.computeProviderId = computeProviderId;
  }

  /**
   * Gets instance count.
   *
   * @return the instance count
   */
  public int getInstanceCount() {
    return instanceCount;
  }

  /**
   * Sets instance count.
   *
   * @param instanceCount the instance count
   */
  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  /**
   * Is specific hosts boolean.
   *
   * @return the boolean
   */
  public boolean isSpecificHosts() {
    return specificHosts;
  }

  /**
   * Sets specific hosts.
   *
   * @param specificHosts the specific hosts
   */
  public void setSpecificHosts(boolean specificHosts) {
    this.specificHosts = specificHosts;
  }

  /**
   * Gets host names.
   *
   * @return the host names
   */
  public List<String> getHostNames() {
    return hostNames;
  }

  /**
   * Sets host names.
   *
   * @param hostNames the host names
   */
  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }
}
