package software.wings.sm.states;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.EXISTS;
import static io.harness.beans.SearchFilter.Operator.NOT_EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.api.AwsCodeDeployRequestElement.AwsCodeDeployRequestElementBuilder.anAwsCodeDeployRequestElement;
import static software.wings.api.PhaseStepExecutionData.PhaseStepExecutionDataBuilder.aPhaseStepExecutionData;
import static software.wings.api.ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.AmiStepExecutionSummary;
import software.wings.api.AwsCodeDeployRequestElement;
import software.wings.api.AwsLambdaContextElement;
import software.wings.api.ClusterElement;
import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.HelmDeployContextElement;
import software.wings.api.HelmSetupExecutionSummary;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.RouteUpdateRollbackElement;
import software.wings.api.ScriptStateExecutionSummary;
import software.wings.api.ServiceInstanceArtifactParam;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.api.pcf.PcfDeployExecutionSummary;
import software.wings.api.pcf.PcfRouteSwapExecutionSummary;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.beans.Activity;
import software.wings.beans.FailureStrategy;
import software.wings.beans.PhaseStepType;
import software.wings.common.Constants;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ElementNotifyResponseData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.ExecutionStatusData;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.StepExecutionSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by rishi on 1/12/17.
 */
public class PhaseStepSubWorkflow extends SubWorkflowState {
  @Inject private ActivityService activityService;

  @Transient @Inject private transient WorkflowExecutionService workflowExecutionService;
  @Transient @Inject private transient StateExecutionService stateExecutionService;

  private PhaseStepType phaseStepType;
  private boolean stepsInParallel;
  private boolean defaultFailureStrategy;
  private List<FailureStrategy> failureStrategies = new ArrayList<>();

  // Only for rollback phases
  @SchemaIgnore private String phaseStepNameForRollback;
  @SchemaIgnore private ExecutionStatus statusForRollback;
  @SchemaIgnore private boolean artifactNeeded;

  public PhaseStepSubWorkflow(String name) {
    super(name, StateType.PHASE_STEP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext contextIntf) {
    if (phaseStepType == null) {
      throw new InvalidRequestException("null phaseStepType");
    }

    ExecutionResponse response;
    PhaseElement phaseElement = contextIntf.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    if (!isRollback()) {
      validatePreRequisites(contextIntf, phaseElement);
      response = super.execute(contextIntf);
    } else if (phaseStepType == PhaseStepType.ROLLBACK_PROVISIONERS) {
      response = (SpawningExecutionResponse) super.execute(contextIntf);
    } else {
      List<ContextElement> rollbackRequiredParams = getRollbackRequiredParam(phaseStepType, phaseElement, contextIntf);
      if (rollbackRequiredParams == null) {
        response = new ExecutionResponse();
      } else {
        SpawningExecutionResponse spawningExecutionResponse = (SpawningExecutionResponse) super.execute(contextIntf);
        for (StateExecutionInstance instance : spawningExecutionResponse.getStateExecutionInstanceList()) {
          rollbackRequiredParams.forEach(p -> instance.getContextElements().push(p));
        }
        response = spawningExecutionResponse;
      }
    }

    response.setStateExecutionData(aPhaseStepExecutionData()
                                       .withStepsInParallel(stepsInParallel)
                                       .withDefaultFailureStrategy(defaultFailureStrategy)
                                       .withFailureStrategies(failureStrategies)
                                       .withPhaseStepType(phaseStepType)
                                       .build());
    return response;
  }

  private List<ContextElement> getRollbackRequiredParam(
      PhaseStepType phaseStepType, PhaseElement phaseElement, ExecutionContext contextIntf) {
    ExecutionContextImpl context = (ExecutionContextImpl) contextIntf;

    PhaseExecutionData stateExecutionData = (PhaseExecutionData) stateExecutionService.phaseStateExecutionData(
        context.getAppId(), context.getWorkflowExecutionId(), phaseElement.getPhaseNameForRollback());

    if (stateExecutionData == null) {
      return null;
    }
    PhaseExecutionSummary phaseExecutionSummary = stateExecutionData.getPhaseExecutionSummary();
    if (phaseExecutionSummary == null || phaseExecutionSummary.getPhaseStepExecutionSummaryMap() == null
        || phaseExecutionSummary.getPhaseStepExecutionSummaryMap().get(phaseStepNameForRollback) == null) {
      return null;
    }
    PhaseStepExecutionSummary phaseStepExecutionSummary =
        phaseExecutionSummary.getPhaseStepExecutionSummaryMap().get(phaseStepNameForRollback);
    if (phaseStepExecutionSummary.getStepExecutionSummaryList() == null) {
      return null;
    }

    switch (phaseStepType) {
      case DISABLE_SERVICE:
      case DEPLOY_SERVICE:
      case STOP_SERVICE:
      case START_SERVICE:
      case ENABLE_SERVICE:
      case VERIFY_SERVICE:
        // Needs service instance id param
        List<String> serviceInstanceIds = phaseStepExecutionSummary.getStepExecutionSummaryList()
                                              .stream()
                                              .filter(s -> s.getElement() != null)
                                              .map(s -> s.getElement().getUuid())
                                              .distinct()
                                              .collect(toList());

        List<ContextElement> contextParams =
            Lists.newArrayList(aServiceInstanceIdsParam().withInstanceIds(serviceInstanceIds).build());

        if (artifactNeeded) {
          ServiceInstanceArtifactParam serviceInstanceArtifactParam = buildInstanceArtifactParam(
              contextIntf.getAppId(), contextIntf.getWorkflowExecutionId(), serviceInstanceIds);
          contextParams.add(serviceInstanceArtifactParam);
        }
        return contextParams;
      case CONTAINER_DEPLOY: {
        Optional<StepExecutionSummary> first = phaseStepExecutionSummary.getStepExecutionSummaryList()
                                                   .stream()
                                                   .filter(s -> s instanceof CommandStepExecutionSummary)
                                                   .findFirst();
        if (!first.isPresent()) {
          //
          // Deploy Container Step can use ShellScript to deploy, we have to let rollback steps go through for that.
          //
          Optional<StepExecutionSummary> firstScriptStateExecutionSummary =
              phaseStepExecutionSummary.getStepExecutionSummaryList()
                  .stream()
                  .filter(s -> s instanceof ScriptStateExecutionSummary)
                  .findFirst();
          if (firstScriptStateExecutionSummary.isPresent()) {
            return singletonList(ContainerRollbackRequestElement.builder()
                                     .oldInstanceData(Collections.EMPTY_LIST)
                                     .newInstanceData(Collections.EMPTY_LIST)
                                     .build());
          }

          return null;
        }
        CommandStepExecutionSummary commandStepExecutionSummary = (CommandStepExecutionSummary) first.get();
        return singletonList(ContainerRollbackRequestElement.builder()
                                 .oldInstanceData(reverse(commandStepExecutionSummary.getNewInstanceData()))
                                 .newInstanceData(reverse(commandStepExecutionSummary.getOldInstanceData()))
                                 .build());
      }
      case DEPLOY_AWSCODEDEPLOY: {
        Optional<StepExecutionSummary> first = phaseStepExecutionSummary.getStepExecutionSummaryList()
                                                   .stream()
                                                   .filter(s -> s instanceof CommandStepExecutionSummary)
                                                   .findFirst();
        if (!first.isPresent()) {
          return null;
        }
        CommandStepExecutionSummary commandStepExecutionSummary = (CommandStepExecutionSummary) first.get();
        AwsCodeDeployRequestElement deployRequestElement =
            anAwsCodeDeployRequestElement()
                .withCodeDeployParams(commandStepExecutionSummary.getCodeDeployParams())
                .withOldCodeDeployParams(commandStepExecutionSummary.getOldCodeDeployParams())
                .build();
        return singletonList(deployRequestElement);
      }
      case DEPLOY_AWS_LAMBDA:
        return new ArrayList<>();
      case AMI_DEPLOY_AUTOSCALING_GROUP: {
        Optional<StepExecutionSummary> first = phaseStepExecutionSummary.getStepExecutionSummaryList()
                                                   .stream()
                                                   .filter(s -> s instanceof AmiStepExecutionSummary)
                                                   .findFirst();
        if (!first.isPresent()) {
          return null;
        }
        AmiStepExecutionSummary amiStepExecutionSummary = (AmiStepExecutionSummary) first.get();
        return asList(amiStepExecutionSummary.getRollbackAmiServiceElement());
      }
      case CONTAINER_SETUP: {
        Optional<StepExecutionSummary> first = phaseStepExecutionSummary.getStepExecutionSummaryList()
                                                   .stream()
                                                   .filter(s -> s instanceof CommandStepExecutionSummary)
                                                   .findFirst();
        if (!first.isPresent()) {
          return null;
        }
        CommandStepExecutionSummary commandStepExecutionSummary = (CommandStepExecutionSummary) first.get();
        return singletonList(
            ContainerRollbackRequestElement.builder()
                .controllerNamePrefix(commandStepExecutionSummary.getControllerNamePrefix())
                .previousEcsServiceSnapshotJson(commandStepExecutionSummary.getPreviousEcsServiceSnapshotJson())
                .ecsServiceArn(commandStepExecutionSummary.getEcsServiceArn())
                .build());
      }
      case ROUTE_UPDATE: {
        return singletonList(RouteUpdateRollbackElement.builder().build());
      }
      case HELM_DEPLOY: {
        Optional<StepExecutionSummary> first = phaseStepExecutionSummary.getStepExecutionSummaryList()
                                                   .stream()
                                                   .filter(s -> s instanceof HelmSetupExecutionSummary)
                                                   .findFirst();
        if (!first.isPresent()) {
          return null;
        }
        HelmSetupExecutionSummary helmSetpExecutionSummary = (HelmSetupExecutionSummary) first.get();
        return asList(HelmDeployContextElement.builder()
                          .releaseName(helmSetpExecutionSummary.getReleaseName())
                          .previousReleaseRevision(helmSetpExecutionSummary.getPrevVersion())
                          .newReleaseRevision(helmSetpExecutionSummary.getNewVersion())
                          .build());
      }
      case PCF_RESIZE: {
        Optional<StepExecutionSummary> first = phaseStepExecutionSummary.getStepExecutionSummaryList()
                                                   .stream()
                                                   .filter(s -> s instanceof PcfDeployExecutionSummary)
                                                   .findFirst();
        if (!first.isPresent()) {
          return null;
        }
        PcfDeployExecutionSummary pcfDeployExecutionSummary = (PcfDeployExecutionSummary) first.get();
        return asList(pcfDeployExecutionSummary.getPcfDeployContextForRollback());
      }
      case PCF_SWICH_ROUTES:
      case PCF_ROUTE_UPDATE: {
        Optional<StepExecutionSummary> first = phaseStepExecutionSummary.getStepExecutionSummaryList()
                                                   .stream()
                                                   .filter(s -> s instanceof PcfRouteSwapExecutionSummary)
                                                   .findFirst();
        if (!first.isPresent()) {
          return null;
        }
        PcfRouteSwapExecutionSummary pcfRouteSwapExecutionSummary = (PcfRouteSwapExecutionSummary) first.get();
        return asList(pcfRouteSwapExecutionSummary.getPcfRouteSwapContextForRollback());
      }
      case AMI_SWITCH_AUTOSCALING_GROUP_ROUTES: {
        // All the data required is already there on the service setup element.
        // We don't need to pass something unnecessarily.
        return emptyList();
      }
      default:
        unhandled(phaseStepType);
    }
    return null;
  }

  private List<ContainerServiceData> reverse(List<ContainerServiceData> serviceCounts) {
    return serviceCounts.stream()
        .map(sc
            -> ContainerServiceData.builder()
                   .name(sc.getName())
                   .image(sc.getImage())
                   .previousCount(sc.getDesiredCount())
                   .desiredCount(sc.getPreviousCount())
                   .previousTraffic(sc.getDesiredTraffic())
                   .desiredTraffic(sc.getPreviousTraffic())
                   .build())
        .collect(toList());
  }

  private ServiceInstanceArtifactParam buildInstanceArtifactParam(
      String appId, String workflowExecutionId, List<String> serviceInstanceIds) {
    if (serviceInstanceIds == null) {
      return null;
    }
    ServiceInstanceArtifactParam serviceInstanceArtifactParam = new ServiceInstanceArtifactParam();
    serviceInstanceIds.forEach(serviceInstanceId -> {
      PageResponse<Activity> pageResponse =
          activityService.list(aPageRequest()
                                   .withLimit("1")
                                   .addFilter("appId", EQ, appId)
                                   .addFilter("serviceInstanceId", EQ, serviceInstanceId)
                                   .addFilter("status", EQ, ExecutionStatus.SUCCESS)
                                   .addFilter("workflowExecutionId", NOT_EQ, workflowExecutionId)
                                   .addFilter("artifactId", EXISTS)
                                   .build());

      if (isNotEmpty(pageResponse)) {
        serviceInstanceArtifactParam.getInstanceArtifactMap().put(
            serviceInstanceId, pageResponse.getResponse().get(0).getArtifactId());
      }
    });

    return serviceInstanceArtifactParam;
  }

  private void validatePreRequisites(ExecutionContext contextIntf, PhaseElement phaseElement) {
    switch (phaseStepType) {
      case DEPLOY_SERVICE:
      case ENABLE_SERVICE:
      case DISABLE_SERVICE: {
        validateServiceInstanceIdsParams(contextIntf);
        break;
      }

      case CONTAINER_DEPLOY:
      case SELECT_NODE:
      case INFRASTRUCTURE_NODE:
      case VERIFY_SERVICE:
      case WRAP_UP:
      case PRE_DEPLOYMENT:
      case POST_DEPLOYMENT:
      case STOP_SERVICE:
      case DE_PROVISION_NODE:
      case CLUSTER_SETUP:
      case CONTAINER_SETUP:
      case START_SERVICE:
      case DEPLOY_AWSCODEDEPLOY:
      case PREPARE_STEPS:
      case DEPLOY_AWS_LAMBDA:
      case COLLECT_ARTIFACT:
      case AMI_AUTOSCALING_GROUP_SETUP:
      case AMI_DEPLOY_AUTOSCALING_GROUP:
      case HELM_DEPLOY:
      case PCF_SETUP:
      case PCF_RESIZE:
      case PCF_ROUTE_UPDATE:
      case PCF_SWICH_ROUTES:
      case ROUTE_UPDATE:
      case K8S_PHASE_STEP:
        noop();
        break;

      default:
        unhandled(phaseStepType);
    }
  }

  private void validateServiceInstanceIdsParams(ExecutionContext contextIntf) {}

  private void validateServiceElement(ExecutionContext context, PhaseElement phaseElement) {
    List<ContextElement> contextElements = context.getContextElementList(ContextElementType.CONTAINER_SERVICE);
    if (isEmpty(contextElements)) {
      throw new InvalidRequestException("Setup not done");
    }
    Optional<ContextElement> containerServiceElement =
        contextElements.parallelStream()
            .filter(contextElement
                -> contextElement instanceof ContainerServiceElement && contextElement.getUuid() != null
                    && contextElement.getUuid().equals(phaseElement.getServiceElement().getUuid()))
            .findFirst();

    if (!containerServiceElement.isPresent()) {
      throw new InvalidRequestException(
          "containerServiceElement not present for the service " + phaseElement.getServiceElement().getUuid());
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    if (phaseStepType == PhaseStepType.PRE_DEPLOYMENT || phaseStepType == PhaseStepType.POST_DEPLOYMENT) {
      ExecutionStatus executionStatus =
          ((ExecutionStatusData) response.values().iterator().next()).getExecutionStatus();
      if (executionStatus != ExecutionStatus.SUCCESS) {
        executionResponse.setExecutionStatus(executionStatus);
      }
      ResponseData notifiedResponseData = response.values().iterator().next();
      if (notifiedResponseData instanceof ElementNotifyResponseData) {
        ElementNotifyResponseData elementNotifyResponseData = (ElementNotifyResponseData) notifiedResponseData;
        List<ContextElement> elements = elementNotifyResponseData.getContextElements();
        if (isNotEmpty(elements)) {
          executionResponse.setContextElements(Lists.newArrayList(elements));
        }
      }
      return executionResponse;
    }

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    if (phaseElement != null) {
      handleElementNotifyResponseData(phaseElement, response, executionResponse);
    }
    PhaseStepExecutionData phaseStepExecutionData = (PhaseStepExecutionData) context.getStateExecutionData();
    phaseStepExecutionData.setPhaseStepExecutionSummary(workflowExecutionService.getPhaseStepExecutionSummary(
        context.getAppId(), context.getWorkflowExecutionId(), context.getStateExecutionInstanceId()));
    executionResponse.setStateExecutionData(phaseStepExecutionData);
    super.handleStatusSummary(workflowExecutionService, context, response, executionResponse);
    return executionResponse;
  }

  private void handleElementNotifyResponseData(
      PhaseElement phaseElement, Map<String, ResponseData> response, ExecutionResponse executionResponse) {
    if (isEmpty(response)) {
      throw new InvalidRequestException("Missing response");
    }
    ResponseData notifiedResponseData = response.values().iterator().next();
    if (!(notifiedResponseData instanceof ElementNotifyResponseData)) {
      throw new InvalidRequestException("Response data has wrong type");
    }
    ElementNotifyResponseData elementNotifyResponseData = (ElementNotifyResponseData) notifiedResponseData;
    if (ExecutionStatus.isNegativeStatus(elementNotifyResponseData.getExecutionStatus())) {
      return;
    }

    String deploymentType = phaseElement.getDeploymentType();
    if (deploymentType == null) {
      return;
    }

    if (deploymentType.equals(DeploymentType.AWS_LAMBDA.name()) && phaseStepType == PhaseStepType.DEPLOY_AWS_LAMBDA) {
      AwsLambdaContextElement awsLambdaContextElement = (AwsLambdaContextElement) notifiedElement(
          elementNotifyResponseData, AwsLambdaContextElement.class, "Missing AwsLambdaContextElement");
      executionResponse.setContextElements(Lists.newArrayList(awsLambdaContextElement));
    } else if ((deploymentType.equals(DeploymentType.SSH.name()) || deploymentType.equals(DeploymentType.WINRM.name()))
        && phaseStepType == PhaseStepType.INFRASTRUCTURE_NODE) {
      ServiceInstanceIdsParam serviceInstanceIdsParam = (ServiceInstanceIdsParam) notifiedElement(
          elementNotifyResponseData, ServiceInstanceIdsParam.class, "Missing ServiceInstanceIdsParam");
      executionResponse.setContextElements(Lists.newArrayList(serviceInstanceIdsParam));
    } else if (phaseStepType == PhaseStepType.CLUSTER_SETUP) {
      ClusterElement clusterElement =
          (ClusterElement) notifiedElement(elementNotifyResponseData, ClusterElement.class, "Missing ClusterElement");
      executionResponse.setContextElements(singletonList(clusterElement));
      executionResponse.setNotifyElements(singletonList(clusterElement));
    } else if (phaseStepType == PhaseStepType.CONTAINER_SETUP) {
      if (!isEmpty(elementNotifyResponseData.getContextElements())) {
        ContainerServiceElement containerServiceElement = (ContainerServiceElement) notifiedElement(
            elementNotifyResponseData, ContainerServiceElement.class, "Missing ContainerServiceElement");
        executionResponse.setContextElements(singletonList(containerServiceElement));
        executionResponse.setNotifyElements(singletonList(containerServiceElement));
      }
    } else if (phaseStepType == PhaseStepType.CONTAINER_DEPLOY) {
      if (!isEmpty(elementNotifyResponseData.getContextElements())) {
        InstanceElementListParam instanceElementListParam = (InstanceElementListParam) notifiedElement(
            elementNotifyResponseData, InstanceElementListParam.class, "Missing InstanceListParam Element");
        executionResponse.setContextElements(Lists.newArrayList(instanceElementListParam));
      }
    } else if (phaseStepType == PhaseStepType.DEPLOY_AWSCODEDEPLOY) {
      InstanceElementListParam instanceElementListParam = (InstanceElementListParam) notifiedElement(
          elementNotifyResponseData, InstanceElementListParam.class, "Missing InstanceListParam Element");
      executionResponse.setContextElements(Lists.newArrayList(instanceElementListParam));
    } else if (phaseStepType == PhaseStepType.AMI_AUTOSCALING_GROUP_SETUP) {
      AmiServiceSetupElement amiServiceElement = (AmiServiceSetupElement) notifiedElement(
          elementNotifyResponseData, AmiServiceSetupElement.class, "Missing AmiServiceElement Element");
      executionResponse.setContextElements(Lists.newArrayList(amiServiceElement));
      executionResponse.setNotifyElements(Lists.newArrayList(amiServiceElement));
    } else if (phaseStepType == PhaseStepType.AMI_DEPLOY_AUTOSCALING_GROUP) {
      InstanceElementListParam instanceElementListParam = (InstanceElementListParam) notifiedElement(
          elementNotifyResponseData, InstanceElementListParam.class, "Missing InstanceElementListParam Element");
      executionResponse.setContextElements(Lists.newArrayList(instanceElementListParam));
    } else if (phaseStepType == PhaseStepType.HELM_DEPLOY) {
      InstanceElementListParam instanceElementListParam = (InstanceElementListParam) notifiedElement(
          elementNotifyResponseData, InstanceElementListParam.class, "Missing InstanceElementListParam Element");
      executionResponse.setContextElements(Lists.newArrayList(instanceElementListParam));
    } else if (phaseStepType == PhaseStepType.PCF_SETUP) {
      PcfSetupContextElement pcfSetupContextElement = (PcfSetupContextElement) notifiedElement(
          elementNotifyResponseData, PcfSetupContextElement.class, "Missing PcfSetupContextElement");
      executionResponse.setContextElements(Lists.newArrayList(pcfSetupContextElement));
      executionResponse.setNotifyElements(Lists.newArrayList(pcfSetupContextElement));
    } else if (phaseStepType == PhaseStepType.PCF_RESIZE) {
      InstanceElementListParam instanceElementListParam = (InstanceElementListParam) notifiedElement(
          elementNotifyResponseData, InstanceElementListParam.class, "Missing InstanceElementListParam Element");
      executionResponse.setContextElements(Lists.newArrayList(instanceElementListParam));
    }
  }

  private ContextElement notifiedElement(
      ElementNotifyResponseData elementNotifyResponseData, Class<? extends ContextElement> cls, String message) {
    List<ContextElement> elements = elementNotifyResponseData.getContextElements();
    if (isEmpty(elements)) {
      throw new InvalidRequestException(message);
    }
    if (!(cls.isInstance(elements.get(0)))) {
      throw new InvalidRequestException(message);
    }

    return elements.get(0);
  }

  public boolean isStepsInParallel() {
    return stepsInParallel;
  }

  public void setStepsInParallel(boolean stepsInParallel) {
    this.stepsInParallel = stepsInParallel;
  }

  public boolean isDefaultFailureStrategy() {
    return defaultFailureStrategy;
  }

  public void setDefaultFailureStrategy(boolean defaultFailureStrategy) {
    this.defaultFailureStrategy = defaultFailureStrategy;
  }

  public List<FailureStrategy> getFailureStrategies() {
    return failureStrategies;
  }

  public void setFailureStrategies(List<FailureStrategy> failureStrategies) {
    this.failureStrategies = failureStrategies;
  }

  @SchemaIgnore
  public String getPhaseStepNameForRollback() {
    return phaseStepNameForRollback;
  }

  public void setPhaseStepNameForRollback(String phaseStepNameForRollback) {
    this.phaseStepNameForRollback = phaseStepNameForRollback;
  }

  @SchemaIgnore
  public ExecutionStatus getStatusForRollback() {
    return statusForRollback;
  }

  public void setStatusForRollback(ExecutionStatus statusForRollback) {
    this.statusForRollback = statusForRollback;
  }

  @SchemaIgnore
  public PhaseStepType getPhaseStepType() {
    return phaseStepType;
  }

  public void setPhaseStepType(PhaseStepType phaseStepType) {
    this.phaseStepType = phaseStepType;
  }

  @SchemaIgnore
  public boolean isArtifactNeeded() {
    return artifactNeeded;
  }

  public void setArtifactNeeded(boolean artifactNeeded) {
    this.artifactNeeded = artifactNeeded;
  }
}
