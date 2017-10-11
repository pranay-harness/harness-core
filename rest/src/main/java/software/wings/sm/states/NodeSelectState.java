package software.wings.sm.states;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.api.ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam;
import static software.wings.beans.InstanceUnitType.COUNT;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.ServiceInstanceSelectionParams.Builder.aServiceInstanceSelectionParams;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.api.SelectedNodeExecutionData;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by brett on 10/10/17
 */
public abstract class NodeSelectState extends State {
  private static final Logger logger = LoggerFactory.getLogger(NodeSelectState.class);

  private List<String> hostNames;

  @Inject @Transient private InfrastructureMappingService infrastructureMappingService;

  NodeSelectState(String name, String stateType) {
    super(name, stateType);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      String appId = ((ExecutionContextImpl) context).getApp().getUuid();

      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      String serviceId = phaseElement.getServiceElement().getUuid();
      String infraMappingId = phaseElement.getInfraMappingId();

      List<ServiceInstance> hostExclusionList = CanaryUtils.getHostExclusionList(context, phaseElement);
      List<String> excludedServiceInstanceIds =
          hostExclusionList.stream().map(ServiceInstance::getUuid).distinct().collect(toList());

      InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
      ServiceInstanceSelectionParams.Builder selectionParams =
          aServiceInstanceSelectionParams()
              .withExcludedServiceInstanceIds(excludedServiceInstanceIds)
              .withSelectSpecificHosts(isSpecificHosts());
      int totalAvailableInstances = infrastructureMappingService.listHostNames(appId, infraMappingId).size();
      int instancesToAdd;
      if (isSpecificHosts()) {
        if (infrastructureMapping instanceof AwsInfrastructureMapping
            && ((AwsInfrastructureMapping) infrastructureMapping).isProvisionInstances()) {
          throw new WingsException(
              ErrorCode.INVALID_ARGUMENT, "args", "Cannot specify hosts when using an auto scale group");
        }
        selectionParams.withHostNames(getHostNames());
        instancesToAdd = getHostNames().size();
        logger.info("Selecting specific hosts: {}", getHostNames());
      } else {
        instancesToAdd = getCumulativeTotal(totalAvailableInstances) - hostExclusionList.size();
      }
      selectionParams.withCount(instancesToAdd);

      logger.info(
          "Selected {} instances - serviceId: {}, infraMappingId: {}", instancesToAdd, serviceId, infraMappingId);
      List<ServiceInstance> serviceInstances =
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
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", e.getMessage(), e);
    }
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
      List<ServiceInstance> hostExclusionList, InfrastructureMapping infraMapping, int totalAvailableInstances) {
    String errorMessage = null;
    if (isEmpty(serviceInstances)) {
      StringBuilder msg = new StringBuilder("No nodes were selected. ");
      if (isSpecificHosts()) {
        msg.append("'Use Specific Hosts' was chosen ");
        if (isEmpty(getHostNames())) {
          msg.append("but no host names were specified. ");
        } else {
          msg.append("with these host names: ").append(getHostNames()).append(". ");
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
          .append(infraMapping.getDisplayName())
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
      } else if (infraMapping instanceof AwsInfrastructureMapping) {
        AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infraMapping;
        if (awsInfrastructureMapping.isProvisionInstances()) {
          msg.append("your Auto Scale group [")
              .append(awsInfrastructureMapping.getAutoScalingGroupName())
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
  public Map<String, String> validateFields() {
    Map<String, String> invalidFieldMessages = new HashMap<>();
    if (isSpecificHosts()) {
      if (isEmpty(getHostNames())) {
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

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public List<String> getHostNames() {
    return hostNames;
  }

  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }

  public abstract boolean isSpecificHosts();

  public abstract int getInstanceCount();

  public abstract InstanceUnitType getInstanceUnitType();
}
