package software.wings.service.impl.trigger;

import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.USER;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.govern.Switch.unhandled;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.service.impl.workflow.WorkflowServiceTemplateHelper.getServiceWorkflowVariables;
import static software.wings.service.impl.workflow.WorkflowServiceTemplateHelper.getTemplatizedEnvVariableName;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.DeploymentTriggerExecutionArgs;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.deployment.DeploymentMetadata.Include;
import software.wings.beans.trigger.Action.ActionType;
import software.wings.beans.trigger.Condition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.GitHubPayloadSource;
import software.wings.beans.trigger.PayloadSource.Type;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerExecution;
import software.wings.beans.trigger.TriggerExecution.Status;
import software.wings.beans.trigger.WebhookCondition;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.dl.WingsPersistence;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.trigger.TriggerExecutionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class TriggerDeploymentExecution {
  @Inject private transient WingsPersistence wingsPersistence;
  @Inject private transient ServiceVariableService serviceVariablesService;
  @Inject private transient PipelineService pipelineService;
  @Inject private transient WorkflowService workflowService;
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient WorkflowExecutionService workflowExecutionService;
  @Inject private transient EnvironmentService environmentService;
  @Inject private transient WebhookTriggerProcessor webhookTriggerProcessor;
  @Inject private transient InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private transient TriggerExecutionService triggerExecutionService;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private transient ArtifactService artifactService;
  @Inject private transient TriggerArtifactVariableHandler triggerArtifactVariableHandler;
  @Inject private FeatureFlagService featureFlagService;

  public WorkflowExecution triggerDeployment(List<ArtifactVariable> artifactVariables,
      Map<String, String> webhookParameters, DeploymentTrigger deploymentTrigger, TriggerExecution triggerExecution) {
    if (deploymentTrigger.isTriggerDisabled()) {
      logger.warn("Trigger is disabled for appId {}, Trigger Id {} and name {} for token {} ",
          deploymentTrigger.getAppId(), deploymentTrigger.getUuid(), deploymentTrigger.getWebHookToken());
      return null;
    }
    ExecutionArgs executionArgs = new ExecutionArgs();
    if (isNotEmpty(artifactVariables)) {
      logger.info("The artifact variables set for the trigger {} are {}", deploymentTrigger.getUuid(),
          artifactVariables.stream().map(ArtifactVariable::getName).collect(toList()));
      executionArgs.setArtifactVariables(artifactVariables);
    }

    if (isNotEmpty(webhookParameters)) {
      executionArgs.setWorkflowVariables(webhookParameters);
    }
    executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
    executionArgs.setTriggerExecutionArgs(DeploymentTriggerExecutionArgs.builder()
                                              .triggerName(deploymentTrigger.getName())
                                              .triggerUuid(deploymentTrigger.getUuid())
                                              .build());

    switch (deploymentTrigger.getAction().getActionType()) {
      case PIPELINE:
        PipelineAction pipelineAction = (PipelineAction) deploymentTrigger.getAction();
        executionArgs.setOrchestrationId(pipelineAction.getPipelineId());
        executionArgs.setWorkflowType(PIPELINE);
        executionArgs.setPipelineId(pipelineAction.getPipelineId());
        executionArgs.setExcludeHostsWithSameArtifact(pipelineAction.getTriggerArgs().isExcludeHostsWithSameArtifact());

        return triggerPipelineDeployment(deploymentTrigger, triggerExecution, executionArgs);

      case WORKFLOW:
        WorkflowAction workflowAction = (WorkflowAction) deploymentTrigger.getAction();
        executionArgs.setOrchestrationId(workflowAction.getWorkflowId());
        executionArgs.setWorkflowType(ORCHESTRATION);
        executionArgs.setExcludeHostsWithSameArtifact(workflowAction.getTriggerArgs().isExcludeHostsWithSameArtifact());

        return triggerOrchestrationDeployment(deploymentTrigger, executionArgs, triggerExecution);

      default:
        unhandled(deploymentTrigger.getAction().getActionType());
    }

    logger.info("Trigger {} executed deployment successfully in app {}", deploymentTrigger.getName(),
        deploymentTrigger.getAppId());

    return null;
  }

  public WorkflowExecution executeDeployment(DeploymentTrigger trigger, List<ArtifactVariable> artifactVariables) {
    if (trigger.getAction() != null) {
      if (isNotEmpty(artifactVariables)) {
        logger.info("The artifact variables set for the trigger {} are {}", trigger.getUuid(),
            artifactVariables.stream().map(ArtifactVariable::getName).collect(toList()));
      }
      try {
        return triggerDeployment(artifactVariables, null, trigger, null);
      } catch (WingsException exception) {
        exception.addContext(Application.class, trigger.getAppId());
        exception.addContext(DeploymentTrigger.class, trigger.getUuid());
        ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      }
    } else {
      logger.info("No action exist. Hence Skipping the deployment");
      return null;
    }

    return null;
  }

  private void matchTriggerAndDeploymentArtifactVariables(String trigerId, String appId,
      List<ArtifactVariable> triggerArtifactVariables, List<ArtifactVariable> deploymentArtifactVariables) {
    if (isEmpty(triggerArtifactVariables) && isNotEmpty(deploymentArtifactVariables)) {
      logger.error(
          "Some artifact variables {} do not exist in trigger {} but they exist in pipeline/Workflow for app {}",
          deploymentArtifactVariables, trigerId, appId);
      throw new TriggerException(
          "Some artifact variables do not exist in trigger but they exist in pipeline/Workflow", null);
    }

    if (isEmpty(triggerArtifactVariables)) {
      return;
    }

    Set<String> triggerArtifactVariableNames =
        triggerArtifactVariables.stream().map(ArtifactVariable::getName).collect(toSet());

    if (deploymentArtifactVariables != null) {
      List<String> artifactVariables =
          deploymentArtifactVariables.stream()
              .filter(deploymentVariable -> deploymentVariable.getType() == VariableType.ARTIFACT)
              .filter(deploymentVariable -> !triggerArtifactVariableNames.contains(deploymentVariable.getName()))
              .map(Variable::getName)
              .collect(Collectors.toList());

      if (isNotEmpty(artifactVariables)) {
        logger.error("Some artifact variables {} do not exist in trigger {} but they exist in pipeline for app {}",
            artifactVariables, trigerId, appId);
        throw new TriggerException("Some artifact variables do not exist in trigger but they exist in pipeline", null);
      }
    }
  }

  private WorkflowExecution triggerPipelineDeployment(
      DeploymentTrigger trigger, TriggerExecution triggerExecution, ExecutionArgs executionArgs) {
    WorkflowExecution workflowExecution;

    PipelineAction pipelineAction = (PipelineAction) trigger.getAction();
    logger.info(
        "Triggering  execution of appId {} with  pipeline id {}", trigger.getAppId(), pipelineAction.getPipelineId());
    boolean infraDefEnabled = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, trigger.getAccountId());
    resolveTriggerPipelineVariables(trigger, executionArgs, infraDefEnabled);
    if (checkFileContentOptionSelected(trigger)) {
      workflowExecution = webhookTriggerPipelineExecution(trigger, triggerExecution, executionArgs);
    } else {
      workflowExecution = workflowExecutionService.triggerEnvExecution(trigger.getAppId(), null, executionArgs, null);
    }
    logger.info("Pipeline execution of appId {} with  pipeline id {} triggered", trigger.getAppId(),
        pipelineAction.getPipelineId());
    return workflowExecution;
  }

  private WorkflowExecution webhookTriggerPipelineExecution(
      DeploymentTrigger trigger, TriggerExecution triggerExecution, ExecutionArgs executionArgs) {
    logger.info("Check file content option selected. Invoking delegate task to verify the file content.");
    // Harsh TODO add webhook condition
    TriggerExecution lastTriggerExecution =
        webhookTriggerProcessor.fetchLastExecutionForContentChanged(Trigger.builder().build());
    if (lastTriggerExecution == null) {
      triggerExecution.setExecutionArgs(executionArgs);
      triggerExecution.setStatus(Status.SUCCESS);
      triggerExecutionService.save(triggerExecution);
      return workflowExecutionService.triggerEnvExecution(trigger.getAppId(), null, executionArgs, null);
    } else {
      triggerExecution.setExecutionArgs(executionArgs);
      webhookTriggerProcessor.initiateTriggerContentChangeDelegateTask(
          null, lastTriggerExecution, triggerExecution, trigger.getAppId());
      return WorkflowExecution.builder().status(ExecutionStatus.NEW).build();
    }
  }

  public boolean checkFileContentOptionSelected(DeploymentTrigger trigger) {
    Condition condition = trigger.getCondition();
    if (condition instanceof WebhookCondition) {
      WebhookCondition webHookTriggerCondition = (WebhookCondition) condition;
      if (webHookTriggerCondition.getPayloadSource().getType() == Type.GITHUB) {
        GitHubPayloadSource gitHubPayloadSource = (GitHubPayloadSource) webHookTriggerCondition.getPayloadSource();
        return gitHubPayloadSource.getWebhookGitParam() != null;
      } else {
        return false;
      }
    }
    return false;
  }

  private void resolveTriggerPipelineVariables(
      DeploymentTrigger deploymentTrigger, ExecutionArgs executionArgs, boolean infraDefEnabled) {
    PipelineAction pipelineAction = (PipelineAction) deploymentTrigger.getAction();
    Pipeline pipeline =
        pipelineService.readPipeline(deploymentTrigger.getAppId(), pipelineAction.getPipelineId(), true);
    notNullCheck("Pipeline was deleted or does not exist", pipeline, USER);

    List<Variable> pipelineVariables = pipeline.getPipelineVariables();
    Map<String, String> triggerPipelineVariableValues =
        overrideTriggerVariables(deploymentTrigger, executionArgs, infraDefEnabled);

    String envId = null;
    String templatizedEnvName = getTemplatizedEnvVariableName(pipelineVariables);
    if (templatizedEnvName != null) {
      logger.info("One of the environment is parameterized in the pipeline and Variable name {}", templatizedEnvName);
      String envNameOrId = triggerPipelineVariableValues.get(templatizedEnvName);
      if (envNameOrId == null) {
        String msg = "Pipeline contains environment as variable [" + templatizedEnvName
            + "]. However, there is no mapping associated in the trigger."
            + " Please update the trigger";

        logger.error(msg + " triggerId  {} appId {}", deploymentTrigger.getUuid(), deploymentTrigger.getAppId());
        throw new TriggerException(msg, USER);
      }
      envId = resolveEnvId(deploymentTrigger.getAppId(), envNameOrId);
      triggerPipelineVariableValues.put(templatizedEnvName, envId);
    }

    resolveServices(deploymentTrigger.getAppId(), triggerPipelineVariableValues, pipelineVariables);

    if (infraDefEnabled) {
      resolveInfraDefinitions(deploymentTrigger.getAppId(), triggerPipelineVariableValues, envId, pipelineVariables);
    } else {
      resolveServiceInfrastructures(
          deploymentTrigger.getAppId(), triggerPipelineVariableValues, envId, pipelineVariables);
    }

    executionArgs.setWorkflowVariables(triggerPipelineVariableValues);

    DeploymentMetadata deploymentMetadata = pipelineService.fetchDeploymentMetadata(
        deploymentTrigger.getAppId(), pipeline, null, null, Include.ARTIFACT_SERVICE);

    List<ArtifactVariable> artifactVariables = deploymentMetadata.getArtifactVariables();
    if (deploymentTrigger.getType() == Condition.Type.WEBHOOK) {
      artifactVariables = deploymentMetadata.getArtifactVariables();
      if (artifactVariables != null && pipelineAction.getTriggerArgs() != null) {
        artifactVariables.forEach(artifactVariable -> {
          String value = triggerArtifactVariableHandler.fetchArtifactVariableValue(deploymentTrigger.getAppId(),
              pipelineAction.getTriggerArgs().getTriggerArtifactVariables(), artifactVariable, deploymentTrigger, null);
          artifactVariable.setValue(value);
        });
      }
    }

    executionArgs.setArtifactVariables(artifactVariables);

    /*matchTriggerAndDeploymentArtifactVariables(deploymentTrigger.getUuid(), deploymentTrigger.getAppId(),
        executionArgs.getArtifactVariables(), deploymentMetadata.getArtifactVariables());*/

    List<String> artifactNeededServiceIds = isEmpty(pipeline.getServices())
        ? new ArrayList<>()
        : pipeline.getServices().stream().map(Service::getUuid).collect(toList());

    validateRequiredArtifacts(deploymentTrigger, executionArgs, artifactNeededServiceIds);
  }

  private void resolveServices(String appId, Map<String, String> triggerVariableValues, List<Variable> variables) {
    List<String> serviceWorkflowVariables = getServiceWorkflowVariables(variables);
    for (String serviceVarName : serviceWorkflowVariables) {
      String serviceIdOrName = triggerVariableValues.get(serviceVarName);
      notNullCheck("There is no corresponding Workflow Variable associated to service", serviceIdOrName);
      logger.info("Checking  service {} can be found by id first.", serviceIdOrName);
      Service service = serviceResourceService.get(appId, serviceIdOrName, false);
      if (service == null) {
        logger.error("Service does not exist by Id, checking if environment {} can be found by name.", serviceIdOrName);
        service = serviceResourceService.getServiceByName(appId, serviceIdOrName, false);
      }
      notNullCheck("Service [" + serviceIdOrName + "] does not exist", service, USER);
      triggerVariableValues.put(serviceVarName, service.getUuid());
    }
  }

  private List<String> obtainCollectedArtifactServiceIds(ExecutionArgs executionArgs) {
    final List<ArtifactVariable> artifactVariables = executionArgs.getArtifactVariables();
    if (isEmpty(artifactVariables)) {
      return new ArrayList<>();
    }

    Set<String> artifactServiceIds = new HashSet<>();
    artifactVariables.forEach(artifactVariable -> {
      List<String> serviceIds = artifactStreamServiceBindingService.listServiceIds(
          artifactService.get(artifactVariable.getValue()).getArtifactStreamId());
      if (isNotEmpty(serviceIds)) {
        artifactServiceIds.addAll(serviceIds);
      }
    });
    return new ArrayList<>(artifactServiceIds);
  }

  private void validateRequiredArtifacts(
      DeploymentTrigger deploymentTrigger, ExecutionArgs executionArgs, List<String> artifactNeededServiceIds) {
    // Artifact serviceIds
    List<String> collectedArtifactServiceIds = obtainCollectedArtifactServiceIds(executionArgs);

    WorkflowType workflowType = null;
    String executionName = null;
    if (deploymentTrigger.getAction().getActionType() == ActionType.PIPELINE) {
      PipelineAction pipelineAction = (PipelineAction) deploymentTrigger.getAction();
      workflowType = PIPELINE;
      executionName = pipelineAction.getPipelineName();
    } else {
      WorkflowAction workflowAction = (WorkflowAction) deploymentTrigger.getAction();
      workflowType = ORCHESTRATION;
      executionName = workflowAction.getWorkflowName();
    }

    if (isEmpty(artifactNeededServiceIds) && isNotEmpty(collectedArtifactServiceIds)) {
      StringBuilder msg = new StringBuilder(128);
      msg.append("Trigger rejected. Reason: ")
          .append(PIPELINE == workflowType ? "Pipeline" : "Workflow")
          .append(" [")
          .append(executionName)
          .append("] does not need artifacts. However, trigger received with the artifacts");
      logger.warn(msg.toString());
      //      throw new WingsException(msg.toString());
    }
    List<String> missingServiceIds = new ArrayList<>();
    for (String artifactNeededServiceId : artifactNeededServiceIds) {
      if (collectedArtifactServiceIds.contains(artifactNeededServiceId)) {
        collectedArtifactServiceIds.remove(artifactNeededServiceId);
      } else {
        missingServiceIds.add(artifactNeededServiceId);
      }
    }
    if (isNotEmpty(missingServiceIds)) {
      logger.info(
          "Artifact needed serviceIds {} do not match with the collected artifact serviceIds {}. Rejecting the trigger {} execution",
          artifactNeededServiceIds, collectedArtifactServiceIds, deploymentTrigger.getUuid());
      List<String> missingServiceNames =
          serviceResourceService.fetchServiceNamesByUuids(deploymentTrigger.getAppId(), missingServiceIds);
      logger.warn("Trigger rejected. Reason: Artifacts are missing for service name(s) {}", missingServiceNames);
      //      throw new WingsException(
      //          "Trigger rejected. Reason: Artifacts are missing for service name(s)" + missingServiceNames, USER);
    }
    if (isNotEmpty(collectedArtifactServiceIds)) {
      StringBuilder msg =
          new StringBuilder("Trigger rejected. Reason: More artifacts received than required artifacts for ");
      msg.append(PIPELINE == workflowType ? "Pipeline" : "Workflow").append(" [").append(executionName).append(']');
      logger.warn(msg.toString());
      //      throw new WingsException(msg.toString());
    }
  }

  private String resolveEnvId(String appId, String envNameOrId) {
    Environment environment;
    logger.info("Checking  environment {} can be found by id first.", envNameOrId);
    environment = environmentService.get(appId, envNameOrId);
    if (environment == null) {
      logger.info("Environment does not exist by Id, checking if environment {} can be found by name.", envNameOrId);
      environment = environmentService.getEnvironmentByName(appId, envNameOrId, false);
    }
    notNullCheck("Resolved environment [" + envNameOrId
            + "] does not exist. Please ensure the environment variable mapped to the right payload value in the trigger",
        environment, USER);

    return environment.getUuid();
  }

  private Map<String, String> overrideTriggerVariables(
      DeploymentTrigger deploymentTrigger, ExecutionArgs executionArgs, boolean infraDefEnabled) {
    // Workflow variables come from Webhook
    Map<String, String> webhookVariableValues =
        executionArgs.getWorkflowVariables() == null ? new HashMap<>() : executionArgs.getWorkflowVariables();

    Map<String, String> variables = null;
    switch (deploymentTrigger.getAction().getActionType()) {
      case PIPELINE:
        PipelineAction pipelineAction = (PipelineAction) deploymentTrigger.getAction();
        if (pipelineAction.getTriggerArgs().getVariables() != null) {
          variables = pipelineAction.getTriggerArgs()
                          .getVariables()
                          .stream()
                          .filter(variableEntry -> isNotEmpty(variableEntry.getValue()))
                          .collect(Collectors.toMap(Variable::getName, Variable::getValue));
        }
        break;
      case WORKFLOW:
        WorkflowAction workflowAction = (WorkflowAction) deploymentTrigger.getAction();
        if (workflowAction.getTriggerArgs().getVariables() != null) {
          variables = workflowAction.getTriggerArgs()
                          .getVariables()
                          .stream()
                          .filter(variableEntry -> isNotEmpty(variableEntry.getValue()))
                          .collect(Collectors.toMap(Variable::getName, Variable::getValue));
        }
        break;
      default:
        unhandled(deploymentTrigger.getAction().getActionType());
    }

    Map<String, String> triggerWorkflowVariableValues = variables == null ? new HashMap<>() : variables;

    for (Entry<String, String> entry : webhookVariableValues.entrySet()) {
      if (isNotEmpty(entry.getValue())) {
        if (infraDefEnabled && entry.getKey().startsWith("ServiceInfra")) {
          String infraMappingVarName = entry.getKey();
          String infraDefVarName = infraMappingVarName.replace("ServiceInfra", "InfraDefinition");
          triggerWorkflowVariableValues.put(infraDefVarName, entry.getValue());
        } else {
          triggerWorkflowVariableValues.put(entry.getKey(), entry.getValue());
        }
      }
    }
    triggerWorkflowVariableValues = triggerWorkflowVariableValues.entrySet()
                                        .stream()
                                        .filter(variableEntry -> isNotEmpty(variableEntry.getValue()))
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return triggerWorkflowVariableValues;
  }

  private WorkflowExecution triggerOrchestrationDeployment(
      DeploymentTrigger deploymentTrigger, ExecutionArgs executionArgs, TriggerExecution triggerExecution) {
    WorkflowAction workflowAction = (WorkflowAction) deploymentTrigger.getAction();
    logger.info("Triggering  workflow execution of appId {} with with workflow id {}", deploymentTrigger.getAppId(),
        workflowAction.getWorkflowId());

    Workflow workflow = workflowService.readWorkflow(deploymentTrigger.getAppId(), workflowAction.getWorkflowId());
    notNullCheck("Workflow was deleted", workflow, USER);
    notNullCheck("Orchestration Workflow not present", workflow.getOrchestrationWorkflow(), USER);
    boolean infraDefEnabled = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, workflow.getAccountId());

    List<Variable> workflowVariables = workflow.getOrchestrationWorkflow().getUserVariables();
    Map<String, String> triggerWorkflowVariableValues =
        overrideTriggerVariables(deploymentTrigger, executionArgs, infraDefEnabled);

    String envId = null;
    if (BUILD == workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType()) {
      executionArgs.setArtifactVariables(new ArrayList<>());
    } else {
      envId =
          resolveEnvironment(deploymentTrigger.getAppId(), workflow, workflowVariables, triggerWorkflowVariableValues);
      resolveServices(deploymentTrigger.getAppId(), triggerWorkflowVariableValues, workflowVariables);
      resolveServiceInfrastructures(
          deploymentTrigger.getAppId(), triggerWorkflowVariableValues, envId, workflowVariables);

      if (infraDefEnabled) {
        resolveInfraDefinitions(deploymentTrigger.getAppId(), triggerWorkflowVariableValues, envId, workflowVariables);
      } else {
        resolveServiceInfrastructures(
            deploymentTrigger.getAppId(), triggerWorkflowVariableValues, envId, workflowVariables);
      }

      /* Fetch the deployment data to find out the required entity types */
      DeploymentMetadata deploymentMetadata = workflowService.fetchDeploymentMetadata(
          deploymentTrigger.getAppId(), workflow, triggerWorkflowVariableValues, null, null, Include.ARTIFACT_SERVICE);

      List<ArtifactVariable> artifactVariables = deploymentMetadata.getArtifactVariables();
      if (deploymentTrigger.getType() == Condition.Type.WEBHOOK) {
        // We have to find artifact variable value because of resolved variables here
        artifactVariables = deploymentMetadata.getArtifactVariables();
        if (artifactVariables != null && workflowAction.getTriggerArgs() != null) {
          artifactVariables.forEach(artifactVariable -> {
            String value = triggerArtifactVariableHandler.fetchArtifactVariableValue(deploymentTrigger.getAppId(),
                workflowAction.getTriggerArgs().getTriggerArtifactVariables(), artifactVariable, deploymentTrigger,
                null);
            artifactVariable.setValue(value);
          });
        }
        executionArgs.setArtifactVariables(artifactVariables);
      }

      // Todo harsh validate with trigger args
      //      matchTriggerAndDeploymentArtifactVariables(deploymentTrigger.getUuid(), deploymentTrigger.getAppId(),
      //          triggerArtifactVariables, deploymentMetadata.getArtifactVariables());

      // Fetch the service
      List<String> artifactNeededServiceIds =
          deploymentMetadata == null ? new ArrayList<>() : deploymentMetadata.getArtifactRequiredServiceIds();
      validateRequiredArtifacts(deploymentTrigger, executionArgs, artifactNeededServiceIds);
    }

    executionArgs.setWorkflowVariables(triggerWorkflowVariableValues);

    // Validate if the file path content changed
    logger.info("Triggering workflow execution of appId {} with workflow id {} triggered", deploymentTrigger.getAppId(),
        workflowAction.getWorkflowId());

    return executeWorkflow(deploymentTrigger, executionArgs, envId, triggerExecution);
  }

  private WorkflowExecution executeWorkflow(DeploymentTrigger deploymentTrigger, ExecutionArgs executionArgs,
      String envId, TriggerExecution triggerExecution) {
    WorkflowExecution workflowExecution;
    if (checkFileContentOptionSelected(deploymentTrigger)) {
      // TODO add webhook condition
      TriggerExecution lastTriggerExecution = webhookTriggerProcessor.fetchLastExecutionForContentChanged(null);
      if (lastTriggerExecution == null) {
        triggerExecution.setStatus(Status.SUCCESS);
        triggerExecution.setExecutionArgs(executionArgs);
        triggerExecution.setEnvId(envId);
        triggerExecutionService.save(triggerExecution);
        workflowExecution =
            workflowExecutionService.triggerEnvExecution(deploymentTrigger.getAppId(), envId, executionArgs, null);
      } else {
        logger.info("Check file content option selected. Invoking delegate task to verify the file content.");
        triggerExecution.setExecutionArgs(executionArgs);
        webhookTriggerProcessor.initiateTriggerContentChangeDelegateTask(
            null, lastTriggerExecution, triggerExecution, deploymentTrigger.getAppId());
        workflowExecution = WorkflowExecution.builder().status(ExecutionStatus.NEW).build();
      }
    } else {
      workflowExecution =
          workflowExecutionService.triggerEnvExecution(deploymentTrigger.getAppId(), envId, executionArgs, null);
    }
    return workflowExecution;
  }
  private String resolveEnvironment(String appId, Workflow workflow, List<Variable> workflowVariables,
      Map<String, String> triggerWorkflowVariableValues) {
    String envId;
    if (workflow.checkEnvironmentTemplatized()) {
      String templatizedEnvName = getTemplatizedEnvVariableName(workflowVariables);
      String envNameOrId = triggerWorkflowVariableValues.get(templatizedEnvName);
      notNullCheck(
          "Workflow Environment is templatized. However, there is no corresponding mapping associated in the trigger. "
              + " Please update the trigger",
          envNameOrId, USER);
      envId = resolveEnvId(appId, envNameOrId);
      triggerWorkflowVariableValues.put(templatizedEnvName, envId);
    } else {
      envId = workflow.getEnvId();
    }
    notNullCheck("Environment  [" + envId + "] might have been deleted", envId, USER);
    return envId;
  }

  void resolveInfraDefinitions(
      String appId, Map<String, String> triggerWorkflowVariableValues, String envId, List<Variable> variables) {
    for (Variable variable : WorkflowServiceTemplateHelper.getInfraDefCompleteWorkflowVariables(variables)) {
      String infraEnvId = null;
      if (isNotEmpty(variable.getMetadata()) && variable.getMetadata().get(Variable.ENV_ID) != null) {
        infraEnvId = variable.getMetadata().get(Variable.ENV_ID).toString();
      } else {
        infraEnvId = envId;
      }

      String infraDefVarName = variable.getName();
      String infraDefIdOrName = triggerWorkflowVariableValues.get(infraDefVarName);
      if (isEmpty(infraDefVarName) || matchesVariablePattern(infraDefIdOrName)) {
        String infraMappingVarName = infraDefVarName.replace("InfraDefinition", "ServiceInfra");
        String infraMappingIdOrName = triggerWorkflowVariableValues.get(infraMappingVarName);
        InfrastructureMapping infrastructureMapping = getInfrastructureMapping(appId, infraEnvId, infraMappingIdOrName);
        notNullCheck(
            "Service Infrastructure [" + infraMappingIdOrName + "] does not exist", infrastructureMapping, USER);
        triggerWorkflowVariableValues.put(infraDefVarName, infrastructureMapping.getInfrastructureDefinitionId());
      } else {
        InfrastructureDefinition infrastructureDefinition =
            getInfrastructureDefinition(appId, infraEnvId, infraDefIdOrName);
        if (infrastructureDefinition != null) {
          triggerWorkflowVariableValues.put(infraDefVarName, infrastructureDefinition.getUuid());
        } else {
          InfrastructureMapping infrastructureMapping = getInfrastructureMapping(appId, infraEnvId, infraDefIdOrName);
          notNullCheck("Service Infrastructure [" + infraDefIdOrName + "] does not exist", infrastructureMapping, USER);
          triggerWorkflowVariableValues.put(infraDefVarName, infrastructureMapping.getInfrastructureDefinitionId());
        }
      }
    }
  }

  public InfrastructureDefinition getInfrastructureDefinition(String appId, String envId, String infraDefIdOrName) {
    InfrastructureDefinition infrastructureDefinition = infrastructureDefinitionService.get(appId, infraDefIdOrName);
    if (infrastructureDefinition == null) {
      logger.info("InfraDefinition does not exist by Id, checking if infra definition {} can be found by name.",
          infraDefIdOrName);
      infrastructureDefinition = infrastructureDefinitionService.getInfraDefByName(appId, envId, infraDefIdOrName);
    }
    return infrastructureDefinition;
  }

  public InfrastructureMapping getInfrastructureMapping(String appId, String envId, String infraDefIdOrName) {
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraDefIdOrName);
    if (infrastructureMapping == null) {
      logger.info(
          "Service Infrastructure does not exist by Id, checking if service infrastructure {} can be found by name.",
          infraDefIdOrName);
      infrastructureMapping = infrastructureMappingService.getInfraMappingByName(appId, envId, infraDefIdOrName);
    }
    return infrastructureMapping;
  }

  private void resolveServiceInfrastructures(
      String appId, Map<String, String> triggerWorkflowVariableValues, String envId, List<Variable> variables) {
    List<String> serviceInfraWorkflowVariables =
        WorkflowServiceTemplateHelper.getServiceInfrastructureWorkflowVariables(variables);
    for (String serviceInfraVarName : serviceInfraWorkflowVariables) {
      String serviceInfraIdOrName = triggerWorkflowVariableValues.get(serviceInfraVarName);
      notNullCheck("There is no corresponding Workflow Variable associated to Service Infrastructure",
          serviceInfraIdOrName, USER);
      logger.info("Checking  Service Infrastructure {} can be found by id first.", serviceInfraIdOrName);
      InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, serviceInfraIdOrName);
      if (infrastructureMapping == null) {
        logger.error(
            "Service Infrastructure does not exist by Id, checking if service infrastructure {} can be found by name.",
            serviceInfraIdOrName);
        infrastructureMapping = infrastructureMappingService.getInfraMappingByName(appId, envId, serviceInfraIdOrName);
      }
      notNullCheck("Service Infrastructure [" + serviceInfraIdOrName + "] does not exist", infrastructureMapping, USER);
      triggerWorkflowVariableValues.put(serviceInfraVarName, infrastructureMapping.getUuid());
    }
  }
}
