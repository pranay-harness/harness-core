package software.wings.sm.states;

import static java.util.stream.Collectors.toList;
import static software.wings.api.ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam;
import static software.wings.beans.ServiceInstanceSelectionParams.Builder.aServiceInstanceSelectionParams;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.api.SelectedNodeExecutionData;
import software.wings.beans.ServiceInstance;
import software.wings.common.Constants;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by rishi on 1/12/17.
 */
public class AwsNodeSelectState extends State {
  private static final Logger logger = LoggerFactory.getLogger(AwsNodeSelectState.class);

  @Attributes(title = "Number of instances") private int instanceCount;

  @Attributes(title = "Select specific hosts?") private boolean specificHosts;
  private List<String> hostNames;

  @Attributes(title = "Provision Nodes?") private boolean provisionNode;
  @Attributes(title = "Host specification (Launch Configuration Name)") private String launcherConfigName;

  @Inject @Transient private InfrastructureMappingService infrastructureMappingService;

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

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    String infraMappingId = phaseElement.getInfraMappingId();

    List<ServiceInstance> serviceInstances;

    if (provisionNode) {
      logger.info("serviceId: {}, environmentId: {}, infraMappingId: {}, instanceCount: {}, launcherConfigName: {}",
          serviceId, envId, infraMappingId, instanceCount, launcherConfigName);
      // TODO - use autoScalingGroupName from aws infra mapping. Check instanceCount vs autoscaling group size
      serviceInstances =
          infrastructureMappingService.provisionNodes(appId, envId, infraMappingId, launcherConfigName, instanceCount);
    } else {
      logger.info(
          "serviceId: {}, environmentId: {}, infraMappingId: {}, instanceCount: {}, specificHosts: {}, hostNames: {}",
          serviceId, envId, infraMappingId, instanceCount, specificHosts, hostNames);
      List<ServiceInstance> hostExclusionList = CanaryUtils.getHostExclusionList(context, phaseElement);
      List<String> excludedServiceInstanceIds =
          hostExclusionList.stream().map(ServiceInstance::getUuid).distinct().collect(toList());

      serviceInstances = infrastructureMappingService.selectServiceInstances(appId, envId, infraMappingId,
          aServiceInstanceSelectionParams()
              .withSelectSpecificHosts(specificHosts)
              .withCount(instanceCount)
              .withHostNames(hostNames)
              .withExcludedServiceInstanceIds(excludedServiceInstanceIds)
              .build());
      if (serviceInstances == null || serviceInstances.isEmpty()) {
        return anExecutionResponse()
            .withExecutionStatus(ExecutionStatus.FAILED)
            .withErrorMessage("No node selected")
            .build();
      }
    }

    SelectedNodeExecutionData selectedNodeExecutionData = new SelectedNodeExecutionData();
    selectedNodeExecutionData.setServiceInstanceList(serviceInstances);

    List<String> serviceInstancesIds = serviceInstances.stream().map(ServiceInstance::getUuid).collect(toList());

    ContextElement serviceIdParamElement =
        aServiceInstanceIdsParam().withInstanceIds(serviceInstancesIds).withServiceId(serviceId).build();
    return anExecutionResponse()
        .addContextElement(serviceIdParamElement)
        .addNotifyElement(serviceIdParamElement)
        .withStateExecutionData(selectedNodeExecutionData)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFieldMessages = new HashMap<>();
    if (isSpecificHosts()) {
      if (hostNames == null || hostNames.isEmpty()) {
        invalidFieldMessages.put(Constants.SELECT_NODE_NAME, "Hostnames must be specified");
      }
    }
    return invalidFieldMessages;
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

  /**
   * Is provision node boolean.
   *
   * @return the boolean
   */
  public boolean isProvisionNode() {
    return provisionNode;
  }

  /**
   * Sets provision node.
   *
   * @param provisionNode the provision node
   */
  public void setProvisionNode(boolean provisionNode) {
    this.provisionNode = provisionNode;
  }

  /**
   * Gets launcher config name.
   *
   * @return the launcher config name
   */
  public String getLauncherConfigName() {
    return launcherConfigName;
  }

  /**
   * Sets launcher config name.
   *
   * @param launcherConfigName the launcher config name
   */
  public void setLauncherConfigName(String launcherConfigName) {
    this.launcherConfigName = launcherConfigName;
  }
}
