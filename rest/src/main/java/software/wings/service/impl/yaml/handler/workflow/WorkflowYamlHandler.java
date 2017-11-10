package software.wings.service.impl.yaml.handler.workflow;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Graph.Node;
import software.wings.beans.NotificationRule;
import software.wings.beans.ObjectType;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.sync.YamlSyncHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.Validator;
import software.wings.yaml.workflow.StepYaml;
import software.wings.yaml.workflow.WorkflowYaml;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/27/17
 */
public abstract class WorkflowYamlHandler<Y extends WorkflowYaml, B extends WorkflowYaml.Builder>
    extends BaseYamlHandler<Y, Workflow> {
  @Inject WorkflowService workflowService;
  @Inject YamlSyncHelper yamlSyncHelper;
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject EnvironmentService environmentService;

  @Override
  public Workflow createFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    WorkflowBuilder workflowBuilder = WorkflowBuilder.aWorkflow();
    setWithYamlValues(changeContext, changeSetContext, workflowBuilder, true);
    return workflowBuilder.build();
  }

  private void setWithYamlValues(ChangeContext<Y> changeContext, List<ChangeContext> changeContextList,
      WorkflowBuilder workflow, boolean isCreate) throws HarnessException {
    WorkflowYaml yaml = changeContext.getYaml();
    Change change = changeContext.getChange();

    String appId = yamlSyncHelper.getAppId(change.getAccountId(), change.getFilePath());
    Validator.notNullCheck("Could not locate app info in file path:" + change.getFilePath(), appId);

    Environment environment = environmentService.getEnvironmentByName(appId, yaml.getEnvName());
    Validator.notNullCheck("Invalid environment with the given name:" + yaml.getEnvName(), environment);

    String envId = environment.getUuid();

    try {
      BaseYamlHandler phaseYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.PHASE, ObjectType.PHASE);
      // phases
      List<WorkflowPhase> phaseList = Lists.newArrayList();
      if (yaml.getPhases() != null) {
        phaseList = yaml.getPhases()
                        .stream()
                        .map(workflowPhaseYaml -> {
                          try {
                            ChangeContext.Builder clonedContext =
                                cloneFileChangeContext(changeContext, workflowPhaseYaml);
                            clonedContext.withEnvId(envId);
                            WorkflowPhase workflowPhase = (WorkflowPhase) createOrUpdateFromYaml(
                                isCreate, phaseYamlHandler, clonedContext.build(), changeContextList);
                            workflowPhase.setRollback(false);
                            return workflowPhase;
                          } catch (HarnessException e) {
                            throw new WingsException(e);
                          }
                        })
                        .collect(Collectors.toList());
      }

      Map<String, WorkflowPhase> workflowPhaseMap =
          phaseList.stream().collect(Collectors.toMap(WorkflowPhase::getName, phase -> phase));

      // rollback phases
      Map<String, WorkflowPhase> rollbackPhaseMap = Maps.newHashMap();
      if (yaml.getRollbackPhases() != null) {
        List<WorkflowPhase> rollbackPhaseList =
            yaml.getRollbackPhases()
                .stream()
                .map(workflowPhaseYaml -> {
                  try {
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, workflowPhaseYaml);
                    clonedContext.withEnvId(envId);
                    WorkflowPhase workflowPhase = (WorkflowPhase) createOrUpdateFromYaml(
                        isCreate, phaseYamlHandler, clonedContext.build(), changeContextList);
                    workflowPhase.setRollback(true);
                    return workflowPhase;
                  } catch (HarnessException e) {
                    throw new WingsException(e);
                  }
                })
                .collect(Collectors.toList());
        rollbackPhaseList.stream().forEach(rollbackPhase -> {
          WorkflowPhase workflowPhase = workflowPhaseMap.get(rollbackPhase.getPhaseNameForRollback());
          if (workflowPhase != null) {
            rollbackPhaseMap.put(workflowPhase.getUuid(), rollbackPhase);
          }
        });
      }

      // user variables
      List<Variable> userVariables = Lists.newArrayList();
      if (yaml.getUserVariables() != null) {
        BaseYamlHandler variableYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.VARIABLE, ObjectType.VARIABLE);
        userVariables = yaml.getUserVariables()
                            .stream()
                            .map(userVariable -> {
                              try {
                                ChangeContext.Builder clonedContext =
                                    cloneFileChangeContext(changeContext, userVariable);
                                return (Variable) createOrUpdateFromYaml(
                                    isCreate, variableYamlHandler, clonedContext.build(), changeContextList);
                              } catch (HarnessException e) {
                                throw new WingsException(e);
                              }
                            })
                            .collect(Collectors.toList());
      }

      // template expressions
      List<TemplateExpression> templateExpressions = Lists.newArrayList();
      if (yaml.getTemplateExpressions() != null) {
        BaseYamlHandler templateExprYamlHandler =
            yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION, ObjectType.TEMPLATE_EXPRESSION);

        templateExpressions = yaml.getTemplateExpressions()
                                  .stream()
                                  .map(templateExpr -> {
                                    try {
                                      ChangeContext.Builder clonedContext =
                                          cloneFileChangeContext(changeContext, templateExpr);
                                      return (TemplateExpression) createOrUpdateFromYaml(
                                          isCreate, templateExprYamlHandler, clonedContext.build(), changeContextList);
                                    } catch (HarnessException e) {
                                      throw new WingsException(e);
                                    }
                                  })
                                  .collect(Collectors.toList());
      }

      BaseYamlHandler stepYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.STEP, ObjectType.STEP);

      // Pre-deployment steps
      PhaseStep.PhaseStepBuilder preDeploymentSteps =
          PhaseStep.PhaseStepBuilder.aPhaseStep(PhaseStepType.PRE_DEPLOYMENT, PhaseStepType.PRE_DEPLOYMENT.name());

      if (yaml.getPreDeploymentSteps() != null) {
        List<Node> stepList = yaml.getPreDeploymentSteps()
                                  .stream()
                                  .map(stepYaml -> {
                                    try {
                                      ChangeContext.Builder clonedContext =
                                          cloneFileChangeContext(changeContext, stepYaml);
                                      return (Node) createOrUpdateFromYaml(
                                          isCreate, stepYamlHandler, clonedContext.build(), changeContextList);
                                    } catch (HarnessException e) {
                                      throw new WingsException(e);
                                    }
                                  })
                                  .collect(Collectors.toList());
        preDeploymentSteps.addAllSteps(stepList).build();
      }

      // Post-deployment steps
      PhaseStep.PhaseStepBuilder postDeploymentSteps =
          PhaseStep.PhaseStepBuilder.aPhaseStep(PhaseStepType.POST_DEPLOYMENT, PhaseStepType.POST_DEPLOYMENT.name());

      if (yaml.getPostDeploymentSteps() != null) {
        List<Node> postDeployStepList =
            yaml.getPostDeploymentSteps()
                .stream()
                .map(stepYaml -> {
                  try {
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, stepYaml);
                    return (Node) createOrUpdateFromYaml(
                        isCreate, stepYamlHandler, clonedContext.build(), changeContextList);
                  } catch (HarnessException e) {
                    throw new WingsException(e);
                  }
                })
                .collect(Collectors.toList());
        postDeploymentSteps.addAllSteps(postDeployStepList).build();
      }

      // Failure strategies
      List<FailureStrategy> failureStrategies = Lists.newArrayList();
      if (yaml.getFailureStrategies() != null) {
        BaseYamlHandler failureStrategyYamlHandler =
            yamlHandlerFactory.getYamlHandler(YamlType.FAILURE_STRATEGY, ObjectType.FAILURE_STRATEGY);
        failureStrategies = yaml.getFailureStrategies()
                                .stream()
                                .map(failureStrategy -> {
                                  try {
                                    ChangeContext.Builder clonedContext =
                                        cloneFileChangeContext(changeContext, failureStrategy);
                                    return (FailureStrategy) createOrUpdateFromYaml(
                                        isCreate, failureStrategyYamlHandler, clonedContext.build(), changeContextList);
                                  } catch (HarnessException e) {
                                    throw new WingsException(e);
                                  }
                                })
                                .collect(Collectors.toList());
      }

      // Notification rules
      List<NotificationRule> notificationRules = Lists.newArrayList();
      if (yaml.getNotificationRules() != null) {
        BaseYamlHandler notificationRuleYamlHandler =
            yamlHandlerFactory.getYamlHandler(YamlType.NOTIFICATION_RULE, ObjectType.NOTIFICATION_RULE);
        notificationRules =
            yaml.getNotificationRules()
                .stream()
                .map(notificationRule -> {
                  try {
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, notificationRule);
                    return (NotificationRule) createOrUpdateFromYaml(
                        isCreate, notificationRuleYamlHandler, clonedContext.build(), changeContextList);
                  } catch (HarnessException e) {
                    throw new WingsException(e);
                  }
                })
                .collect(Collectors.toList());
      }

      WorkflowInfo workflowInfo = WorkflowInfo.builder()
                                      .failureStrategies(failureStrategies)
                                      .notificationRules(notificationRules)
                                      .postDeploymentSteps(postDeploymentSteps.build())
                                      .preDeploymentSteps(preDeploymentSteps.build())
                                      .rollbackPhaseMap(rollbackPhaseMap)
                                      .userVariables(userVariables)
                                      .phaseList(phaseList)
                                      .build();

      OrchestrationWorkflow orchestrationWorkflow = constructOrchestrationWorkflow(workflowInfo);

      workflow.withAppId(appId)
          .withDescription(yaml.getDescription())
          .withDefaultVersion(yaml.getDefaultVersion())
          .withEnvId(envId)
          .withName(yaml.getName())
          .withOrchestrationWorkflow(orchestrationWorkflow)
          .withTemplateExpressions(templateExpressions)
          .withTemplatized(yaml.isTemplatized())
          .withWorkflowType(WorkflowType.ORCHESTRATION);

    } catch (WingsException ex) {
      throw new HarnessException(ex);
    }
  }

  protected abstract OrchestrationWorkflow constructOrchestrationWorkflow(WorkflowInfo workflowInfo);

  @Override
  public Y toYaml(Workflow workflow, String appId) {
    Environment environment = environmentService.get(appId, workflow.getEnvId(), false);
    Validator.notNullCheck("No env found with Id:" + workflow.getEnvId(), environment);

    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhases = orchestrationWorkflow.getWorkflowPhases();

    // phases
    BaseYamlHandler phaseYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.PHASE, ObjectType.PHASE);
    List<WorkflowPhase.Yaml> phaseYamlList =
        workflowPhases.stream()
            .map(workflowPhase -> (WorkflowPhase.Yaml) phaseYamlHandler.toYaml(workflowPhase, appId))
            .collect(Collectors.toList());

    // rollback phases
    Collection<WorkflowPhase> rollbackPhaseCollection = orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().values();
    List<WorkflowPhase.Yaml> rollbackPhaseYamlList =
        rollbackPhaseCollection.stream()
            .map(workflowPhase -> (WorkflowPhase.Yaml) phaseYamlHandler.toYaml(workflowPhase, appId))
            .collect(Collectors.toList());

    // user variables
    List<Variable> userVariables = orchestrationWorkflow.getUserVariables();
    BaseYamlHandler variableYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.VARIABLE, ObjectType.VARIABLE);
    List<Variable.Yaml> variableYamlList =
        userVariables.stream()
            .map(userVariable -> (Variable.Yaml) variableYamlHandler.toYaml(userVariable, appId))
            .collect(Collectors.toList());

    // template expressions
    BaseYamlHandler templateExpressionYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION, ObjectType.TEMPLATE_EXPRESSION);
    List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();
    List<TemplateExpression.Yaml> templateExprYamlList = null;
    if (templateExpressions != null) {
      templateExprYamlList =
          templateExpressions.stream()
              .map(templateExpression
                  -> (TemplateExpression.Yaml) templateExpressionYamlHandler.toYaml(templateExpression, appId))
              .collect(Collectors.toList());
    }

    BaseYamlHandler stepYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.STEP, ObjectType.STEP);
    // Pre-deployment steps
    PhaseStep preDeploymentSteps = orchestrationWorkflow.getPreDeploymentSteps();
    List<StepYaml> preDeployStepsYamlList = preDeploymentSteps.getSteps()
                                                .stream()
                                                .map(step -> (StepYaml) stepYamlHandler.toYaml(step, appId))
                                                .collect(Collectors.toList());

    // Post-deployment steps
    PhaseStep postDeploymentSteps = orchestrationWorkflow.getPostDeploymentSteps();
    List<StepYaml> postDeployStepsYamlList = postDeploymentSteps.getSteps()
                                                 .stream()
                                                 .map(step -> (StepYaml) stepYamlHandler.toYaml(step, appId))
                                                 .collect(Collectors.toList());

    // Failure strategies
    BaseYamlHandler failureStrategyYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.FAILURE_STRATEGY, ObjectType.FAILURE_STRATEGY);
    List<FailureStrategy> failureStrategies = orchestrationWorkflow.getFailureStrategies();
    List<FailureStrategy.Yaml> failureStrategyYamlList =
        failureStrategies.stream()
            .map(failureStrategy -> (FailureStrategy.Yaml) failureStrategyYamlHandler.toYaml(failureStrategy, appId))
            .collect(Collectors.toList());

    // Notification rules
    BaseYamlHandler notificationRuleYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.NOTIFICATION_RULE, ObjectType.NOTIFICATION_RULE);
    List<NotificationRule> notificationRules = orchestrationWorkflow.getNotificationRules();
    List<NotificationRule.Yaml> notificationRuleYamlList =
        notificationRules.stream()
            .map(
                notificationRule -> (NotificationRule.Yaml) notificationRuleYamlHandler.toYaml(notificationRule, appId))
            .collect(Collectors.toList());

    B yamlBuilder = getYamlBuilder();
    return yamlBuilder.withDefaultVersion(workflow.getDefaultVersion())
        .withDescription(workflow.getDescription())
        .withEnvName(environment.getName())
        .withName(workflow.getName())
        .withTemplateExpressions(templateExprYamlList)
        .withTemplatized(workflow.isTemplatized())
        .withType(orchestrationWorkflow.getOrchestrationWorkflowType().name())
        .withPhases(phaseYamlList)
        .withRollbackPhases(rollbackPhaseYamlList)
        .withUserVariables(variableYamlList)
        .withPreDeploymentSteps(preDeployStepsYamlList)
        .withPostDeploymentSteps(postDeployStepsYamlList)
        .withNotificationRules(notificationRuleYamlList)
        .withFailureStrategies(failureStrategyYamlList)
        .build();
  }

  protected abstract B getYamlBuilder();

  @Override
  public Workflow updateFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Workflow previous =
        yamlSyncHelper.getWorkflow(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    WorkflowBuilder workflowBuilder = previous.toBuilder();
    setWithYamlValues(changeContext, changeSetContext, workflowBuilder, false);
    return workflowBuilder.build();
  }

  @Override
  public boolean validate(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return WorkflowYaml.class;
  }

  @Override
  public Workflow get(String accountId, String yamlFilePath) {
    return yamlSyncHelper.getWorkflow(accountId, yamlFilePath);
  }

  @Override
  public Workflow update(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    Workflow workflow = updateFromYaml(changeContext, changeSetContext);
    return workflowService.updateWorkflow(workflow);
  }

  @Data
  @Builder
  protected static class WorkflowInfo {
    private List<FailureStrategy> failureStrategies;
    private List<NotificationRule> notificationRules;
    private PhaseStep preDeploymentSteps;
    private PhaseStep postDeploymentSteps;
    private Map<String, WorkflowPhase> rollbackPhaseMap;
    private List<Variable> userVariables;
    private List<WorkflowPhase> phaseList;
  }
}
