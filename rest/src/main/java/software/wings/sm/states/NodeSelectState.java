package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.atteo.evo.inflector.English.plural;
import static software.wings.api.ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam;
import static software.wings.beans.InstanceUnitType.COUNT;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.OrchestrationWorkflowType.ROLLING;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceInstanceSelectionParams.Builder.aServiceInstanceSelectionParams;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.api.SelectedNodeExecutionData;
import software.wings.api.ServiceInstanceArtifactParam;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceInstanceSelectionParams;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.exception.InvalidRequestException;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Misc;

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

  @Inject @Transient private InstanceService instanceService;

  @Inject @Transient private ArtifactService artifactService;

  @Inject private transient StateExecutionService stateExecutionService;

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

      List<ServiceInstance> hostExclusionList = stateExecutionService.getHostExclusionList(
          ((ExecutionContextImpl) context).getStateExecutionInstance(), phaseElement);

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
        int instanceCountTotal = getCount(totalAvailableInstances);
        if (((ExecutionContextImpl) context).getStateExecutionInstance().getOrchestrationWorkflowType()
            == OrchestrationWorkflowType.ROLLING) {
          instancesToAdd = instanceCountTotal;
        } else {
          instancesToAdd = Math.max(0, instanceCountTotal - hostExclusionList.size());
        }
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

      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      boolean excludeHostsWithSameArtifact = false;
      if (workflowStandardParams != null) {
        excludeHostsWithSameArtifact = workflowStandardParams.isExcludeHostsWithSameArtifact()
            && !ROLLING.equals(context.getOrchestrationWorkflowType());
        if (InfrastructureMappingType.AWS_SSH.name().equals(infrastructureMapping.getInfraMappingType())
            || InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name().equals(
                   infrastructureMapping.getInfraMappingType())
            || InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM.name().equals(
                   infrastructureMapping.getInfraMappingType())) {
          if (excludeHostsWithSameArtifact) {
            serviceInstances =
                excludeHostsWithTheSameArtifactDeployed(context, appId, serviceId, infraMappingId, serviceInstances);
          }
        }
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
        if (!excludeHostsWithSameArtifact) {
          executionResponse.withErrorMessage("No nodes selected");
        } else {
          executionResponse.withErrorMessage("No nodes selected (Nodes already deployed with the same artifact)");
        }
      }
      return executionResponse.build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  private int getCount(int maxInstances) {
    if (instanceUnitType == PERCENTAGE) {
      int percent = Math.min(instanceCount, 100);
      int percentInstanceCount = (int) Math.round(percent * maxInstances / 100.0);
      return Math.max(percentInstanceCount, 1);
    } else {
      return instanceCount;
    }
  }

  private String buildServiceInstancesErrorMessage(List<ServiceInstance> serviceInstances,
      List<ServiceInstance> hostExclusionList, InfrastructureMapping infraMapping, int totalAvailableInstances,
      ExecutionContext context) {
    if (totalAvailableInstances == 0) {
      return "The service infrastructure [" + infraMapping.getName() + "] has no instances available.";
    }

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
        if (instanceUnitType == PERCENTAGE) {
          return null;
        } else {
          msg.append("This phase deploys to ")
              .append(instanceCount)
              .append(plural(" instance", instanceCount))
              .append(" (cumulative) ");
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
          .append(plural(" instance", totalAvailableInstances))
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
      if (account == null
          || (account.getLicenseInfo() != null && isNotEmpty(account.getLicenseInfo().getAccountType())
                 && AccountType.LITE.equals(account.getLicenseInfo().getAccountType()))) {
        errorMessage = "The license for this account does not allow more than "
            + Constants.DEFAULT_CONCURRENT_EXECUTION_INSTANCE_LIMIT
            + " concurrent instance deployments. Please contact Harness Support.";
      }
    }
    return errorMessage;
  }

  private List<ServiceInstance> excludeHostsWithTheSameArtifactDeployed(ExecutionContext context, String appId,
      String serviceId, String inframappingId, List<ServiceInstance> serviceInstances) {
    if (isEmpty(serviceInstances)) {
      return serviceInstances;
    }
    Artifact artifact = findArtifact(context, serviceId);
    if (artifact == null) {
      return serviceInstances;
    }
    PageRequest<Instance> pageRequest = aPageRequest()
                                            .withLimit(PageRequest.UNLIMITED)
                                            .addFilter("appId", EQ, appId)
                                            .addFilter("serviceId", EQ, serviceId)
                                            .addFilter("infraMappingId", EQ, inframappingId)
                                            .addFilter("lastArtifactStreamId", EQ, artifact.getArtifactStreamId())
                                            .addFilter("lastArtifactSourceName", EQ, artifact.getArtifactSourceName())
                                            .addFilter("lastArtifactBuildNum", EQ, artifact.getBuildNo())
                                            .build();
    List<Instance> instances = instanceService.list(pageRequest).getResponse();
    List<String> hostNames =
        instances.stream().map(instance -> instance.getHostInstanceKey().getHostName()).collect(toList());
    return serviceInstances.stream()
        .filter(serviceInstance -> !hostNames.contains(serviceInstance.getHostName()))
        .collect(toList());
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

  private Artifact findArtifact(ExecutionContext context, String serviceId) {
    ContextElement instanceElement = context.getContextElement(ContextElementType.INSTANCE);
    if (instanceElement != null) {
      ServiceInstanceArtifactParam serviceArtifactElement =
          context.getContextElement(ContextElementType.PARAM, Constants.SERVICE_INSTANCE_ARTIFACT_PARAMS);
      if (serviceArtifactElement != null) {
        String artifactId = serviceArtifactElement.getInstanceArtifactMap().get(instanceElement.getUuid());
        if (artifactId != null) {
          return artifactService.get(context.getAppId(), artifactId);
        }
      }
    }

    return ((DeploymentExecutionContext) context).getArtifactForService(serviceId);
  }

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
