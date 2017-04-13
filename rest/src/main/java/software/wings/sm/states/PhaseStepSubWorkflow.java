package software.wings.sm.states;

import static java.util.Arrays.asList;
import static software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder.aContainerServiceElement;
import static software.wings.api.ContainerUpgradeRequestElement.ContainerUpgradeRequestElementBuilder.aContainerUpgradeRequestElement;
import static software.wings.api.PhaseStepExecutionData.PhaseStepExecutionDataBuilder.aPhaseStepExecutionData;
import static software.wings.api.ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam;
import static software.wings.beans.PhaseStepType.CONTAINER_DEPLOY;
import static software.wings.beans.PhaseStepType.DEPLOY_SERVICE;
import static software.wings.beans.PhaseStepType.DISABLE_SERVICE;
import static software.wings.beans.PhaseStepType.ENABLE_SERVICE;
import static software.wings.beans.PhaseStepType.START_SERVICE;
import static software.wings.beans.PhaseStepType.STOP_SERVICE;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.EXISTS;
import static software.wings.beans.SearchFilter.Operator.NOT_EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.Lists;

import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.ContainerServiceElement;
import software.wings.api.ContainerUpgradeRequestElement;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ServiceInstanceArtifactParam;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.beans.Activity;
import software.wings.beans.ErrorCode;
import software.wings.beans.FailureStrategy;
import software.wings.beans.PhaseStepType;
import software.wings.common.Constants;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ElementNotifyResponseData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.StepExecutionSummary;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by rishi on 1/12/17.
 */
public class PhaseStepSubWorkflow extends SubWorkflowState {
  @Inject private ActivityService activityService;

  public PhaseStepSubWorkflow(String name) {
    super(name, StateType.PHASE_STEP.name());
  }

  @Transient @Inject private transient WorkflowExecutionService workflowExecutionService;

  private PhaseStepType phaseStepType;
  private boolean stepsInParallel;
  private boolean defaultFailureStrategy;
  private List<FailureStrategy> failureStrategies = new ArrayList<>();

  // Only for rollback phases
  @SchemaIgnore private String phaseStepNameForRollback;
  @SchemaIgnore private ExecutionStatus statusForRollback;
  @SchemaIgnore private boolean artifactNeeded;

  @Override
  public ExecutionResponse execute(ExecutionContext contextIntf) {
    if (phaseStepType == null) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "null phaseStepType");
    }

    ExecutionResponse response;
    PhaseElement phaseElement = contextIntf.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    if (!isRollback()) {
      validatePreRequisites(contextIntf, phaseElement);
      response = super.execute(contextIntf);
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
                                       .build());
    return response;
  }

  private List<ContextElement> getRollbackRequiredParam(
      PhaseStepType phaseStepType, PhaseElement phaseElement, ExecutionContext contextIntf) {
    ExecutionContextImpl context = (ExecutionContextImpl) contextIntf;
    PhaseExecutionData stateExecutionData =
        (PhaseExecutionData) context.getStateExecutionData(phaseElement.getPhaseNameForRollback());
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

    if (phaseStepType == DISABLE_SERVICE || phaseStepType == DEPLOY_SERVICE || phaseStepType == STOP_SERVICE
        || phaseStepType == START_SERVICE || phaseStepType == ENABLE_SERVICE) {
      // Needs service instance id param
      List<String> serviceInstanceIds = phaseStepExecutionSummary.getStepExecutionSummaryList()
                                            .stream()
                                            .filter(s -> s.getElement() != null)
                                            .map(s -> s.getElement().getUuid())
                                            .distinct()
                                            .collect(Collectors.toList());

      List<ContextElement> contextParams =
          Lists.newArrayList(aServiceInstanceIdsParam().withInstanceIds(serviceInstanceIds).build());
      if (artifactNeeded) {
        ServiceInstanceArtifactParam serviceInstanceArtifactParam = buildInstanceArtifactParam(
            contextIntf.getAppId(), contextIntf.getWorkflowExecutionId(), serviceInstanceIds);
        contextParams.add(serviceInstanceArtifactParam);
      }
      return contextParams;
    } else if (phaseStepType == CONTAINER_DEPLOY) {
      Optional<StepExecutionSummary> first = phaseStepExecutionSummary.getStepExecutionSummaryList()
                                                 .stream()
                                                 .filter(s -> s instanceof CommandStepExecutionSummary)
                                                 .findFirst();
      if (!first.isPresent()) {
        return null;
      }
      CommandStepExecutionSummary commandStepExecutionSummary = (CommandStepExecutionSummary) first.get();
      ContainerServiceElement contextElement =
          aContainerServiceElement()
              .withName(commandStepExecutionSummary.getOldContainerServiceName())
              .withOldName(commandStepExecutionSummary.getNewContainerServiceName())
              .withClusterName(commandStepExecutionSummary.getClusterName())
              .build();

      ContainerUpgradeRequestElement containerUpgradeRequestElement =
          aContainerUpgradeRequestElement()
              .withOldServiceInstanceCount(commandStepExecutionSummary.getNewServicePreviousInstanceCount())
              .withNewServiceInstanceCount(commandStepExecutionSummary.getOldServicePreviousInstanceCount())
              .withContainerServiceElement(contextElement)
              .build();
      return asList(containerUpgradeRequestElement);
    }
    return null;
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

      if (pageResponse != null && !pageResponse.isEmpty()) {
        serviceInstanceArtifactParam.getInstanceArtifactMap().put(
            serviceInstanceId, pageResponse.getResponse().get(0).getArtifactId());
      }
    });

    return serviceInstanceArtifactParam;
  }

  private void validatePreRequisites(ExecutionContext contextIntf, PhaseElement phaseElement) {
    switch (phaseStepType) {
      case CONTAINER_DEPLOY: {
        validateServiceElement(contextIntf, phaseElement);
        break;
      }
      case DEPLOY_SERVICE:
      case ENABLE_SERVICE:
      case DISABLE_SERVICE: {
        validateServiceInstanceIdsParams(contextIntf);
        break;
      }
    }
  }

  private void validateServiceInstanceIdsParams(ExecutionContext contextIntf) {}

  private void validateServiceElement(ExecutionContext context, PhaseElement phaseElement) {
    List<ContextElement> contextElements = context.getContextElementList(ContextElementType.CONTAINER_SERVICE);
    if ((contextElements == null || contextElements.isEmpty())) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Setup not done");
    }
    Optional<ContextElement> containerServiceElement =
        contextElements.parallelStream()
            .filter(contextElement
                -> contextElement instanceof ContainerServiceElement && contextElement.getUuid() != null
                    && contextElement.getUuid().equals(phaseElement.getServiceElement().getUuid()))
            .findFirst();

    if (!containerServiceElement.isPresent()) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
          "containerServiceElement not present for the service " + phaseElement.getServiceElement().getUuid());
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    if (phaseStepType == PhaseStepType.PRE_DEPLOYMENT || phaseStepType == PhaseStepType.POST_DEPLOYMENT) {
      return executionResponse;
    }
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    handleElementNotifyResponseData(context, phaseElement, response, executionResponse);
    PhaseStepExecutionData phaseStepExecutionData = (PhaseStepExecutionData) context.getStateExecutionData();
    phaseStepExecutionData.setPhaseStepExecutionSummary(workflowExecutionService.getPhaseStepExecutionSummary(
        context.getAppId(), context.getWorkflowExecutionId(), context.getStateExecutionInstanceId()));
    executionResponse.setStateExecutionData(phaseStepExecutionData);
    super.handleStatusSummary(workflowExecutionService, context, response, executionResponse);
    return executionResponse;
  }

  private void handleElementNotifyResponseData(ExecutionContext context, PhaseElement phaseElement,
      Map<String, NotifyResponseData> response, ExecutionResponse executionResponse) {
    String deploymentType = phaseElement.getDeploymentType();
    if (deploymentType.equals(DeploymentType.SSH.name()) && phaseStepType == PhaseStepType.PROVISION_NODE) {
      ServiceInstanceIdsParam serviceInstanceIdsParam = (ServiceInstanceIdsParam) notifiedElement(
          response, ServiceInstanceIdsParam.class, "Missing ServiceInstanceIdsParam");

      executionResponse.setContextElements(Lists.newArrayList(serviceInstanceIdsParam));
    } else if (phaseStepType == PhaseStepType.CONTAINER_SETUP) {
      ContainerServiceElement containerServiceElement = (ContainerServiceElement) notifiedElement(
          response, ContainerServiceElement.class, "Missing ContainerServiceElement");
      executionResponse.setContextElements(asList(containerServiceElement));
      executionResponse.setNotifyElements(asList(containerServiceElement));
    } else if (phaseStepType == PhaseStepType.CONTAINER_DEPLOY) {
      InstanceElementListParam instanceElementListParam = (InstanceElementListParam) notifiedElement(
          response, InstanceElementListParam.class, "Missing InstanceListParam Element");
      executionResponse.setContextElements(Lists.newArrayList(instanceElementListParam));
    }
  }

  private ContextElement notifiedElement(
      Map<String, NotifyResponseData> response, Class<? extends ContextElement> cls, String message) {
    if (response == null || response.isEmpty()) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", message);
    }
    NotifyResponseData notifiedResponseData = response.values().iterator().next();

    if (!(notifiedResponseData instanceof ElementNotifyResponseData)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", message);
    }
    ElementNotifyResponseData elementNotifyResponseData = (ElementNotifyResponseData) notifiedResponseData;
    List<ContextElement> elements = elementNotifyResponseData.getContextElements();
    if (elements == null || elements.isEmpty()) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", message);
    }
    if (!(cls.isInstance(elements.get(0)))) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", message);
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
