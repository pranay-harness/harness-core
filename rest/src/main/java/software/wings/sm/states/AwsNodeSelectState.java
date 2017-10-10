package software.wings.sm.states;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.api.ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam;
import static software.wings.beans.InstanceUnitType.COUNT;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.ServiceInstanceSelectionParams.Builder.aServiceInstanceSelectionParams;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.api.SelectedNodeExecutionData;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.ErrorCode;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceInstanceSelectionParams;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by rishi on 1/12/17.
 */
public class AwsNodeSelectState extends State {
  private static final Logger logger = LoggerFactory.getLogger(AwsNodeSelectState.class);

  @Attributes(title = "Instances (cumulative)") private int instanceCount;

  @Attributes(title = "Instance Unit Type (Count/Percent)")
  @EnumData(enumDataProvider = InstanceUnitTypeDataProvider.class)
  @DefaultValue("COUNT")
  private InstanceUnitType instanceUnitType = COUNT;

  @Attributes(title = "Select specific hosts?") private boolean specificHosts;
  private List<String> hostNames;

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

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    String infraMappingId = phaseElement.getInfraMappingId();

    List<ServiceInstance> serviceInstances;
    List<ServiceInstance> hostExclusionList = CanaryUtils.getHostExclusionList(context, phaseElement);
    List<String> excludedServiceInstanceIds =
        hostExclusionList.stream().map(ServiceInstance::getUuid).distinct().collect(toList());

    AwsInfrastructureMapping infrastructureMapping =
        (AwsInfrastructureMapping) infrastructureMappingService.get(appId, infraMappingId);
    ServiceInstanceSelectionParams.Builder selectionParams =
        aServiceInstanceSelectionParams()
            .withExcludedServiceInstanceIds(excludedServiceInstanceIds)
            .withSelectSpecificHosts(specificHosts);
    int totalAvailableInstances = infrastructureMappingService.listHostNames(appId, infraMappingId).size();
    if (specificHosts) {
      if (infrastructureMapping.isProvisionInstances()) {
        throw new WingsException(
            ErrorCode.INVALID_ARGUMENT, "args", "Cannot specify hosts when using an auto scale group");
      }
      int instancesToAdd = hostNames.size();
      selectionParams.withHostNames(hostNames).withCount(instancesToAdd);
      logger.info("Adding {} instances. serviceId: {}, infraMappingId: {}, hostNames: {}", instancesToAdd, serviceId,
          infraMappingId, hostNames);
    } else {
      int instancesToAdd = getCumulativeTotal(totalAvailableInstances) - hostExclusionList.size();
      selectionParams.withCount(instancesToAdd);
      logger.info("Adding {} instances. serviceId: {}, infraMappingId: {}", instancesToAdd, serviceId, infraMappingId);
    }

    serviceInstances =
        infrastructureMappingService.selectServiceInstances(appId, infraMappingId, selectionParams.build());

    String errorMessage = buildServiceInstancesErrorMessage(
        serviceInstances, hostExclusionList, infrastructureMapping, totalAvailableInstances);

    if (isNotEmpty(errorMessage)) {
      return anExecutionResponse().withExecutionStatus(ExecutionStatus.FAILED).withErrorMessage(errorMessage).build();
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

  private int getCumulativeTotal(int maxInstances) {
    if (getInstanceUnitType() == PERCENTAGE) {
      int percent = Math.min(getInstanceCount(), 100);
      int instanceCount = Long.valueOf(Math.round(percent * maxInstances / 100.0)).intValue();
      return Math.max(instanceCount, 1);
    } else {
      return getInstanceCount();
    }
  }

  private String buildServiceInstancesErrorMessage(List<ServiceInstance> serviceInstances,
      List<ServiceInstance> hostExclusionList, AwsInfrastructureMapping infrastructureMapping,
      int totalAvailableInstances) {
    String errorMessage = null;
    if (isEmpty(serviceInstances)) {
      StringBuilder msg = new StringBuilder("No nodes were selected. ");
      if (isSpecificHosts()) {
        msg.append("'Use Specific Hosts' was chosen ");
        if (isEmpty(hostNames)) {
          msg.append("but no host names were specified. ");
        } else {
          msg.append("with these host names: ").append(hostNames).append(". ");
        }
      } else {
        msg.append("A ")
            .append(getInstanceUnitType() == COUNT ? "count" : "percent")
            .append(" of ")
            .append(getInstanceCount())
            .append(" was specified. ");
        if (getInstanceUnitType() == PERCENTAGE) {
          msg.append("This evaluates to ")
              .append(getCumulativeTotal(totalAvailableInstances))
              .append(" instances (cumulative). ");
        }
      }

      msg.append("\n\nThe service infrastructure [")
          .append(infrastructureMapping.getDisplayName())
          .append("] has ")
          .append(totalAvailableInstances)
          .append(" instance")
          .append(totalAvailableInstances == 1 ? "" : "s")
          .append(" available and ")
          .append(hostExclusionList.size())
          .append(hostExclusionList.size() == 1 ? " has" : " have")
          .append(" already been deployed. ");

      msg.append("\n\nCheck whether ");
      if (isSpecificHosts()) {
        msg.append("you've selected a unique set of host names for each phase. ");
      } else {
        if (infrastructureMapping.isProvisionInstances()) {
          msg.append("your Auto Scale group [")
              .append(infrastructureMapping.getAutoScalingGroupName())
              .append("] capacity has changed. ");
        } else {
          msg.append("the filters specified in your service infrastructure are correct. ");
        }
      }
      errorMessage = msg.toString();
    } else if (serviceInstances.size() > totalAvailableInstances) {
      errorMessage =
          "Too many nodes selected. Did you change service infrastructure without updating Select Nodes in the workflow?";
    }
    return errorMessage;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFieldMessages = new HashMap<>();
    if (isSpecificHosts()) {
      if (isEmpty(hostNames)) {
        invalidFieldMessages.put(Constants.SELECT_NODE_NAME, "Hostnames must be specified");
      }
    } else {
      if (getInstanceCount() <= 0) {
        invalidFieldMessages.put(Constants.SELECT_NODE_NAME, "Count or percent must be specified");
      } else if (getInstanceUnitType() == PERCENTAGE && getInstanceCount() > 100) {
        invalidFieldMessages.put(Constants.SELECT_NODE_NAME, "Percent may not be greater than 100");
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

  public InstanceUnitType getInstanceUnitType() {
    return instanceUnitType;
  }

  public void setInstanceUnitType(InstanceUnitType instanceUnitType) {
    this.instanceUnitType = instanceUnitType;
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
