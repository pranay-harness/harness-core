package software.wings.sm.states;

import static java.util.stream.Collectors.toList;
import static software.wings.api.ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam;
import static software.wings.beans.ServiceInstanceSelectionParams.Builder.aServiceInstanceSelectionParams;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
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
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by rishi on 1/12/17.
 */
public class DcNodeSelectState extends State {
  private static final Logger logger = LoggerFactory.getLogger(DcNodeSelectState.class);

  @Attributes(title = "Number of instances") private int instanceCount;
  @Attributes(title = "Select specific hosts?") private boolean specificHosts;
  private List<String> hostNames;

  @Inject private InfrastructureMappingService infrastructureMappingService;

  public DcNodeSelectState(String name) {
    super(name, StateType.DC_NODE_SELECT.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String appId = ((ExecutionContextImpl) context).getApp().getUuid();
    String envId = ((ExecutionContextImpl) context).getEnv().getUuid();

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    String infraMappingId = phaseElement.getInfraMappingId();

    logger.info(
        "serviceId: {}, environmentId: {}, infraMappingId: {}, instanceCount: {}, specificHosts: {}, hostNames: {}",
        serviceId, envId, infraMappingId, instanceCount, specificHosts, hostNames);

    List<ServiceInstance> hostExclusionList = CanaryUtils.getHostExclusionList(context, phaseElement);
    List<String> excludedServiceInstanceIds =
        hostExclusionList.stream().map(ServiceInstance::getUuid).distinct().collect(toList());
    List<ServiceInstance> serviceInstances =
        infrastructureMappingService.selectServiceInstances(appId, envId, infraMappingId,
            aServiceInstanceSelectionParams()
                .withSelectSpecificHosts(specificHosts)
                .withCount(instanceCount)
                .withHostNames(hostNames)
                .withExcludedServiceInstanceIds(excludedServiceInstanceIds)
                .build());

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

  public boolean isSpecificHosts() {
    return specificHosts;
  }

  public void setSpecificHosts(boolean specificHosts) {
    this.specificHosts = specificHosts;
  }

  public int getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  public List<String> getHostNames() {
    return hostNames;
  }

  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }
}
