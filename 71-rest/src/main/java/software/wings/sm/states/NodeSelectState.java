package software.wings.sm.states;

import static io.harness.beans.OrchestrationWorkflowType.ROLLING;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.atteo.evo.inflector.English.plural;
import static software.wings.api.ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam;
import static software.wings.beans.InstanceUnitType.COUNT;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceInstanceSelectionParams.Builder.aServiceInstanceSelectionParams;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.PageRequest;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.PhaseElement;
import software.wings.api.SelectedNodeExecutionData;
import software.wings.api.ServiceInstanceArtifactParam;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceInstanceSelectionParams;
import software.wings.beans.ServiceInstanceSelectionParams.Builder;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brett on 10/10/17
 */
@Slf4j
public abstract class NodeSelectState extends State {
  private static final int DEFAULT_CONCURRENT_EXECUTION_INSTANCE_LIMIT = 10;

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
  @Inject @Transient private FeatureFlagService featureFlagService;
  @Inject @Transient private AppService appService;
  @Inject @Transient private WorkflowExecutionService workflowExecutionService;

  NodeSelectState(String name, String stateType) {
    super(name, stateType);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String appId = requireNonNull(context.getApp()).getUuid();

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    String infraMappingId = context.fetchInfraMappingId();

    List<ServiceInstance> hostExclusionList = stateExecutionService.getHostExclusionList(
        ((ExecutionContextImpl) context).getStateExecutionInstance(), phaseElement, context.fetchInfraMappingId());

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
        throw new InvalidRequestException("Cannot specify hosts when using an auto scale group", WingsException.USER);
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

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    StringBuilder message = new StringBuilder();
    boolean nodesOverriddenFromExecutionHosts = processExecutionHosts(
        appId, selectionParams, workflowStandardParams, message, context.getWorkflowExecutionId());

    logger.info("Selected {} instances - serviceId: {}, infraMappingId: {}", instancesToAdd, serviceId, infraMappingId);
    List<ServiceInstance> serviceInstances = infrastructureMappingService.selectServiceInstances(
        appId, infraMappingId, context.getWorkflowExecutionId(), selectionParams.build());

    String errorMessage = buildServiceInstancesErrorMessage(
        serviceInstances, hostExclusionList, infrastructureMapping, totalAvailableInstances, context);

    if (isNotEmpty(errorMessage) && !nodesOverriddenFromExecutionHosts) {
      return ExecutionResponse.builder().executionStatus(ExecutionStatus.FAILED).errorMessage(errorMessage).build();
    }

    boolean excludeHostsWithSameArtifact = false;
    if (workflowStandardParams != null) {
      excludeHostsWithSameArtifact = workflowStandardParams.isExcludeHostsWithSameArtifact()
          && !ROLLING.equals(context.getOrchestrationWorkflowType());
      if (InfrastructureMappingType.AWS_SSH.name().equals(infrastructureMapping.getInfraMappingType())
          || InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name().equals(
                 infrastructureMapping.getInfraMappingType())
          || InfrastructureMappingType.AZURE_INFRA.name().equals(infrastructureMapping.getInfraMappingType())
          || InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM.name().equals(
                 infrastructureMapping.getInfraMappingType())) {
        if (excludeHostsWithSameArtifact && !nodesOverriddenFromExecutionHosts) {
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
    ExecutionResponseBuilder executionResponse = ExecutionResponse.builder()
                                                     .contextElement(serviceIdParamElement)
                                                     .notifyElement(serviceIdParamElement)
                                                     .stateExecutionData(selectedNodeExecutionData);
    if (isEmpty(serviceInstances)) {
      if (!excludeHostsWithSameArtifact) {
        executionResponse.errorMessage("No nodes selected");
      } else {
        executionResponse.errorMessage("No nodes selected (Nodes already deployed with the same artifact)");
      }
    }
    if (nodesOverriddenFromExecutionHosts) {
      executionResponse.errorMessage(message.toString());
    }

    return executionResponse.build();
  }

  boolean processExecutionHosts(String appId, Builder selectionParams, WorkflowStandardParams workflowStandardParams,
      StringBuilder message, String workflowExecutionId) {
    if (workflowStandardParams != null && isNotEmpty(workflowStandardParams.getExecutionHosts())
        && featureFlagService.isEnabled(FeatureName.DEPLOY_TO_SPECIFIC_HOSTS, appService.getAccountIdByAppId(appId))) {
      List<StateExecutionInstance> stateExecutionInstancesForPhases =
          workflowExecutionService.getStateExecutionInstancesForPhases(workflowExecutionId);
      if (stateExecutionInstancesForPhases.size() == 1) {
        message.append("Targeted nodes have overridden configured nodes");
        List<String> executionHosts = workflowStandardParams.getExecutionHosts();
        selectionParams.withSelectSpecificHosts(true);
        selectionParams.withHostNames(executionHosts);
        selectionParams.withCount(executionHosts.size());
      } else {
        message.append("No nodes selected as targeted nodes have already been deployed");
        selectionParams.withSelectSpecificHosts(true);
        selectionParams.withCount(0);
      }
      return true;
    }
    return false;
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
      return "The service infrastructure [" + infraMapping.getDisplayName() + "] has no instances available.";
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
    } else if (serviceInstances.size() > DEFAULT_CONCURRENT_EXECUTION_INSTANCE_LIMIT) {
      Account account = accountService.get(requireNonNull(context.getApp()).getAccountId());
      if (account == null
          || (account.getLicenseInfo() != null && isNotEmpty(account.getLicenseInfo().getAccountType())
                 && AccountType.COMMUNITY.equals(account.getLicenseInfo().getAccountType()))) {
        errorMessage = "The license for this account does not allow more than "
            + DEFAULT_CONCURRENT_EXECUTION_INSTANCE_LIMIT
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
    // TODO: ASR: change this.
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
        invalidFieldMessages.put(WorkflowServiceHelper.SELECT_NODE_NAME, "Hostnames must be specified");
      }
    } else {
      if (instanceCount <= 0) {
        invalidFieldMessages.put(WorkflowServiceHelper.SELECT_NODE_NAME, "Count or percent must be specified");
      } else if (instanceUnitType == PERCENTAGE && instanceCount > 100) {
        invalidFieldMessages.put(WorkflowServiceHelper.SELECT_NODE_NAME, "Percent may not be greater than 100");
      }
    }
    return invalidFieldMessages;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private Artifact findArtifact(ExecutionContext context, String serviceId) {
    ContextElement instanceElement = context.getContextElement(ContextElementType.INSTANCE);
    if (instanceElement != null) {
      ServiceInstanceArtifactParam serviceArtifactElement = context.getContextElement(
          ContextElementType.PARAM, ServiceInstanceArtifactParam.SERVICE_INSTANCE_ARTIFACT_PARAMS);
      if (serviceArtifactElement != null) {
        String artifactId = serviceArtifactElement.getInstanceArtifactMap().get(instanceElement.getUuid());
        if (artifactId != null) {
          return artifactService.get(artifactId);
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
