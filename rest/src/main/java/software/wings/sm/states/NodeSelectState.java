package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.api.ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam;
import static software.wings.beans.InstanceUnitType.COUNT;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceInstanceSelectionParams.Builder.aServiceInstanceSelectionParams;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.api.SelectedNodeExecutionData;
import software.wings.beans.Account;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceInstanceSelectionParams;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AccountService;
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

/**
 * Created by brett on 10/10/17
 */
public abstract class NodeSelectState extends State {
  private static final Logger logger = LoggerFactory.getLogger(NodeSelectState.class);

  private int instanceCount;
  private InstanceUnitType instanceUnitType = COUNT;
  private boolean specificHosts;
  private List<String> hostNames;
  private boolean excludeSelectedHostsFromFuturePhases;

  @Inject @Transient private InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient private AccountService accountService;

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
              .withSelectSpecificHosts(specificHosts);
      int totalAvailableInstances =
          infrastructureMappingService.listHostDisplayNames(appId, infraMappingId, context.getWorkflowExecutionId())
              .size();
      int instancesToAdd;
      if (specificHosts) {
        if (infrastructureMapping instanceof AwsInfrastructureMapping
            && ((AwsInfrastructureMapping) infrastructureMapping).isProvisionInstances()) {
          throw new WingsException(ErrorCode.INVALID_ARGUMENT)
              .addParam("args", "Cannot specify hosts when using an auto scale group");
        }
        selectionParams.withHostNames(hostNames);
        instancesToAdd = hostNames.size();
        logger.info("Selecting specific hosts: {}", hostNames);
      } else {
        instancesToAdd = getCumulativeTotal(totalAvailableInstances) - hostExclusionList.size();
      }
      selectionParams.withCount(instancesToAdd);

      logger.info(
          "Selected {} instances - serviceId: {}, infraMappingId: {}", instancesToAdd, serviceId, infraMappingId);
      List<ServiceInstance> serviceInstances = infrastructureMappingService.selectServiceInstances(
          appId, infraMappingId, context.getWorkflowExecutionId(), selectionParams.build());

      String errorMessage = buildServiceInstancesErrorMessage(
          serviceInstances, hostExclusionList, infrastructureMapping, totalAvailableInstances, context);

      if (isNotEmpty(errorMessage)) {
        return anExecutionResponse().withExecutionStatus(ExecutionStatus.FAILED).withErrorMessage(errorMessage).build();
      }

      SelectedNodeExecutionData selectedNodeExecutionData = new SelectedNodeExecutionData();
      selectedNodeExecutionData.setServiceInstanceList(serviceInstances.stream()
                                                           .map(serviceInstance
                                                               -> aServiceInstance()
                                                                      .withUuid(serviceInstance.getUuid())
                                                                      .withHostId(serviceInstance.getHostId())
                                                                      .withHostName(serviceInstance.getHostName())
                                                                      .withPublicDns(serviceInstance.getPublicDns())
                                                                      .build())
                                                           .collect(toList()));
      selectedNodeExecutionData.setExcludeSelectedHostsFromFuturePhases(excludeSelectedHostsFromFuturePhases);
      List<String> serviceInstancesIds = serviceInstances.stream().map(ServiceInstance::getUuid).collect(toList());
      ContextElement serviceIdParamElement =
          aServiceInstanceIdsParam().withInstanceIds(serviceInstancesIds).withServiceId(serviceId).build();
      ExecutionResponse.Builder executionResponse = anExecutionResponse()
                                                        .addContextElement(serviceIdParamElement)
                                                        .addNotifyElement(serviceIdParamElement)
                                                        .withStateExecutionData(selectedNodeExecutionData);
      if (isEmpty(serviceInstances)) {
        executionResponse.withErrorMessage("No nodes selected");
      }
      return executionResponse.build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      throw new WingsException(ErrorCode.INVALID_REQUEST, e).addParam("message", e.getMessage());
    }
  }

  private int getCumulativeTotal(int maxInstances) {
    if (instanceUnitType == PERCENTAGE) {
      int percent = Math.min(instanceCount, 100);
      int percentInstanceCount = Long.valueOf(Math.round(percent * maxInstances / 100.0)).intValue();
      return Math.max(percentInstanceCount, 1);
    } else {
      return instanceCount;
    }
  }

  private String buildServiceInstancesErrorMessage(List<ServiceInstance> serviceInstances,
      List<ServiceInstance> hostExclusionList, InfrastructureMapping infraMapping, int totalAvailableInstances,
      ExecutionContext context) {
    String errorMessage = null;
    if (isEmpty(serviceInstances)) {
      StringBuilder msg = new StringBuilder(256);
      msg.append("No nodes were selected. ");
      if (specificHosts) {
        msg.append("'Use Specific Hosts' was chosen ");
        if (isEmpty(hostNames)) {
          msg.append("but no host names were specified. ");
        } else {
          msg.append("with these host names: ").append(hostNames).append(". ");
        }
      } else {
        msg.append("This phase deploys to ");
        if (instanceUnitType == PERCENTAGE) {
          return null;
        } else {
          msg.append(instanceCount).append(" instance").append(instanceCount == 1 ? "" : "s").append(" (cumulative) ");
        }
      }

      msg.append("and ")
          .append(hostExclusionList.size())
          .append(hostExclusionList.size() == 1 ? " instance has" : " instances have")
          .append(" already been deployed. \n\n"
              + "The service infrastructure [")
          .append(infraMapping.getName())
          .append("] has ")
          .append(totalAvailableInstances)
          .append(" instance")
          .append(totalAvailableInstances == 1 ? "" : "s")
          .append(" available. ");

      if (specificHosts) {
        msg.append("\n\nCheck whether you've selected a unique set of host names for each phase. ");
      } else if (infraMapping instanceof AwsInfrastructureMapping) {
        AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infraMapping;
        msg.append("\n\nCheck whether ");
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
    } else if (serviceInstances.size() > Constants.DEFAULT_CONCURRENT_EXECUTION_INSTANCE_LIMIT) {
      Account account = accountService.get(((ExecutionContextImpl) context).getApp().getAccountId());
      if (account == null || account.getLicenseExpiryTime() == 0) {
        errorMessage = "The license for this account does not allow more than "
            + Constants.DEFAULT_CONCURRENT_EXECUTION_INSTANCE_LIMIT
            + " concurrent instance deployments. Please contact Harness Support.";
      }
    }
    return errorMessage;
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFieldMessages = new HashMap<>();
    if (specificHosts) {
      if (isEmpty(hostNames)) {
        invalidFieldMessages.put(Constants.SELECT_NODE_NAME, "Hostnames must be specified");
      }
    } else {
      if (instanceCount <= 0) {
        invalidFieldMessages.put(Constants.SELECT_NODE_NAME, "Count or percent must be specified");
      } else if (instanceUnitType == PERCENTAGE && instanceCount > 100) {
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

  public int getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  public InstanceUnitType getInstanceUnitType() {
    return instanceUnitType;
  }

  public void setInstanceUnitType(InstanceUnitType instanceUnitType) {
    this.instanceUnitType = instanceUnitType;
  }

  public boolean isSpecificHosts() {
    return specificHosts;
  }

  public void setSpecificHosts(boolean specificHosts) {
    this.specificHosts = specificHosts;
  }

  public boolean getExcludeSelectedHostsFromFuturePhases() {
    return excludeSelectedHostsFromFuturePhases;
  }

  public void setExcludeSelectedHostsFromFuturePhases(Boolean excludeSelectedHostsFromFuturePhases) {
    this.excludeSelectedHostsFromFuturePhases = excludeSelectedHostsFromFuturePhases;
  }
}
