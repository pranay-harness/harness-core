package software.wings.service.impl.yaml.handler.workflow;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Graph.Node;
import software.wings.beans.NotificationRule;
import software.wings.beans.ObjectType;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStep.PhaseStepBuilder;
import software.wings.beans.PhaseStepType;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowPhase.Yaml;
import software.wings.beans.WorkflowType;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.notification.NotificationRulesYamlHandler;
import software.wings.service.impl.yaml.handler.template.TemplateExpressionYamlHandler;
import software.wings.service.impl.yaml.handler.variable.VariableYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.Validator;
import software.wings.yaml.workflow.StepYaml;
import software.wings.yaml.workflow.WorkflowYaml;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/27/17
 */
public abstract class WorkflowYamlHandler<Y extends WorkflowYaml> extends BaseYamlHandler<Y, Workflow> {
  @Inject WorkflowService workflowService;
  @Inject YamlHelper yamlHelper;
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject EnvironmentService environmentService;

  protected abstract void setOrchestrationWorkflow(WorkflowInfo workflowInfo, WorkflowBuilder workflowBuilder);

  @Override
  public Workflow upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    Workflow previous = get(accountId, yamlFilePath);

    WorkflowBuilder workflowBuilder = WorkflowBuilder.aWorkflow();
    toBean(changeContext, changeSetContext, workflowBuilder);

    if (previous != null) {
      workflowBuilder.withUuid(previous.getUuid());
      return workflowService.updateWorkflow(workflowBuilder.build());
    } else {
      return workflowService.createWorkflow(workflowBuilder.build());
    }
  }

  private void toBean(ChangeContext<Y> changeContext, List<ChangeContext> changeContextList, WorkflowBuilder workflow)
      throws HarnessException {
    WorkflowYaml yaml = changeContext.getYaml();
    Change change = changeContext.getChange();

    String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());
    Validator.notNullCheck("Could not locate app info in file path:" + change.getFilePath(), appId);

    // Environment can be null in cloned workflows
    Environment environment = environmentService.getEnvironmentByName(appId, yaml.getEnvName());

    String envId = environment != null ? environment.getUuid() : null;

    try {
      WorkflowPhaseYamlHandler phaseYamlHandler =
          (WorkflowPhaseYamlHandler) yamlHandlerFactory.getYamlHandler(YamlType.PHASE, ObjectType.PHASE);

      // phases
      List<WorkflowPhase> phaseList = Lists.newArrayList();
      if (yaml.getPhases() != null) {
        phaseList = yaml.getPhases()
                        .stream()
                        .map(workflowPhaseYaml -> {
                          try {
                            ChangeContext.Builder clonedContextBuilder =
                                cloneFileChangeContext(changeContext, workflowPhaseYaml);
                            ChangeContext clonedContext = clonedContextBuilder.build();
                            clonedContext.getEntityIdMap().put(EntityType.ENVIRONMENT.name(), envId);
                            clonedContext.getProperties().put(YamlConstants.IS_ROLLBACK, false);

                            WorkflowPhase workflowPhase =
                                phaseYamlHandler.upsertFromYaml(clonedContext, changeContextList);
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
                    ChangeContext.Builder clonedContextBuilder =
                        cloneFileChangeContext(changeContext, workflowPhaseYaml);
                    ChangeContext clonedContext = clonedContextBuilder.build();
                    clonedContext.getEntityIdMap().put(EntityType.ENVIRONMENT.name(), envId);
                    clonedContext.getProperties().put(YamlConstants.IS_ROLLBACK, true);

                    WorkflowPhase workflowPhase = phaseYamlHandler.upsertFromYaml(clonedContext, changeContextList);
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
        userVariables =
            yaml.getUserVariables()
                .stream()
                .map(userVariable -> {
                  try {
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, userVariable);
                    return (Variable) variableYamlHandler.upsertFromYaml(clonedContext.build(), changeContextList);
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
                                      return (TemplateExpression) templateExprYamlHandler.upsertFromYaml(
                                          clonedContext.build(), changeContextList);
                                    } catch (HarnessException e) {
                                      throw new WingsException(e);
                                    }
                                  })
                                  .collect(Collectors.toList());
      }

      BaseYamlHandler stepYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.STEP, ObjectType.STEP);

      // Pre-deployment steps
      PhaseStepBuilder preDeploymentSteps =
          PhaseStepBuilder.aPhaseStep(PhaseStepType.PRE_DEPLOYMENT, PhaseStepType.PRE_DEPLOYMENT.name());

      if (yaml.getPreDeploymentSteps() != null) {
        List<Node> stepList =
            yaml.getPreDeploymentSteps()
                .stream()
                .map(stepYaml -> {
                  try {
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, stepYaml);
                    return (Node) stepYamlHandler.upsertFromYaml(clonedContext.build(), changeContextList);
                  } catch (HarnessException e) {
                    throw new WingsException(e);
                  }
                })
                .collect(Collectors.toList());
        preDeploymentSteps.addAllSteps(stepList).build();
      }

      // Post-deployment steps
      PhaseStepBuilder postDeploymentSteps =
          PhaseStepBuilder.aPhaseStep(PhaseStepType.POST_DEPLOYMENT, PhaseStepType.POST_DEPLOYMENT.name());

      if (yaml.getPostDeploymentSteps() != null) {
        List<Node> postDeployStepList =
            yaml.getPostDeploymentSteps()
                .stream()
                .map(stepYaml -> {
                  try {
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, stepYaml);
                    return (Node) stepYamlHandler.upsertFromYaml(clonedContext.build(), changeContextList);
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
                                    return (FailureStrategy) failureStrategyYamlHandler.upsertFromYaml(
                                        clonedContext.build(), changeContextList);
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
        notificationRules = yaml.getNotificationRules()
                                .stream()
                                .map(notificationRule -> {
                                  try {
                                    ChangeContext.Builder clonedContext =
                                        cloneFileChangeContext(changeContext, notificationRule);
                                    return (NotificationRule) notificationRuleYamlHandler.upsertFromYaml(
                                        clonedContext.build(), changeContextList);
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

      setOrchestrationWorkflow(workflowInfo, workflow);

      String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
      workflow.withAppId(appId)
          .withDescription(yaml.getDescription())
          .withEnvId(envId)
          .withName(name)
          .withTemplateExpressions(templateExpressions)
          .withTemplatized(yaml.isTemplatized())
          .withWorkflowType(WorkflowType.ORCHESTRATION);

    } catch (WingsException ex) {
      throw new HarnessException(ex);
    }
  }

  protected void toYaml(Y yaml, Workflow workflow, String appId) {
    // Environment can be null in case of incomplete cloned workflows
    String envName = null;
    if (isNotBlank(workflow.getEnvId())) {
      Environment environment = environmentService.get(appId, workflow.getEnvId(), false);
      envName = environment != null ? environment.getName() : null;
    }

    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhases = orchestrationWorkflow.getWorkflowPhases();

    // phases
    WorkflowPhaseYamlHandler phaseYamlHandler =
        (WorkflowPhaseYamlHandler) yamlHandlerFactory.getYamlHandler(YamlType.PHASE, ObjectType.PHASE);
    List<WorkflowPhase.Yaml> phaseYamlList = workflowPhases.stream()
                                                 .map(workflowPhase -> phaseYamlHandler.toYaml(workflowPhase, appId))
                                                 .collect(Collectors.toList());

    // rollback phases
    Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = orchestrationWorkflow.getRollbackWorkflowPhaseIdMap();
    List<WorkflowPhase.Yaml> rollbackPhaseYamlList = Lists.newArrayList();
    orchestrationWorkflow.getWorkflowPhaseIds().stream().forEach(workflowPhaseId -> {
      WorkflowPhase rollbackPhase = rollbackWorkflowPhaseIdMap.get(workflowPhaseId);
      if (rollbackPhase != null) {
        Yaml rollbackPhaseYaml = phaseYamlHandler.toYaml(rollbackPhase, appId);
        rollbackPhaseYamlList.add(rollbackPhaseYaml);
      }
    });

    // user variables
    List<Variable> userVariables = orchestrationWorkflow.getUserVariables();
    VariableYamlHandler variableYamlHandler =
        (VariableYamlHandler) yamlHandlerFactory.getYamlHandler(YamlType.VARIABLE, ObjectType.VARIABLE);
    List<Variable.Yaml> variableYamlList = userVariables.stream()
                                               .map(userVariable -> variableYamlHandler.toYaml(userVariable, appId))
                                               .collect(Collectors.toList());

    // template expressions
    TemplateExpressionYamlHandler templateExpressionYamlHandler =
        (TemplateExpressionYamlHandler) yamlHandlerFactory.getYamlHandler(
            YamlType.TEMPLATE_EXPRESSION, ObjectType.TEMPLATE_EXPRESSION);
    List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();
    List<TemplateExpression.Yaml> templateExprYamlList = null;
    if (templateExpressions != null) {
      templateExprYamlList =
          templateExpressions.stream()
              .map(templateExpression -> templateExpressionYamlHandler.toYaml(templateExpression, appId))
              .collect(Collectors.toList());
    }

    StepYamlHandler stepYamlHandler =
        (StepYamlHandler) yamlHandlerFactory.getYamlHandler(YamlType.STEP, ObjectType.STEP);
    // Pre-deployment steps
    PhaseStep preDeploymentSteps = orchestrationWorkflow.getPreDeploymentSteps();
    List<StepYaml> preDeployStepsYamlList = preDeploymentSteps.getSteps()
                                                .stream()
                                                .map(step -> stepYamlHandler.toYaml(step, appId))
                                                .collect(Collectors.toList());

    // Post-deployment steps
    PhaseStep postDeploymentSteps = orchestrationWorkflow.getPostDeploymentSteps();
    List<StepYaml> postDeployStepsYamlList = postDeploymentSteps.getSteps()
                                                 .stream()
                                                 .map(step -> stepYamlHandler.toYaml(step, appId))
                                                 .collect(Collectors.toList());

    // Failure strategies
    FailureStrategyYamlHandler failureStrategyYamlHandler =
        (FailureStrategyYamlHandler) yamlHandlerFactory.getYamlHandler(
            YamlType.FAILURE_STRATEGY, ObjectType.FAILURE_STRATEGY);
    List<FailureStrategy> failureStrategies = orchestrationWorkflow.getFailureStrategies();
    List<FailureStrategy.Yaml> failureStrategyYamlList =
        failureStrategies.stream()
            .map(failureStrategy -> failureStrategyYamlHandler.toYaml(failureStrategy, appId))
            .collect(Collectors.toList());

    // Notification rules
    NotificationRulesYamlHandler notificationRuleYamlHandler =
        (NotificationRulesYamlHandler) yamlHandlerFactory.getYamlHandler(
            YamlType.NOTIFICATION_RULE, ObjectType.NOTIFICATION_RULE);
    List<NotificationRule> notificationRules = orchestrationWorkflow.getNotificationRules();
    List<NotificationRule.Yaml> notificationRuleYamlList =
        notificationRules.stream()
            .map(notificationRule -> notificationRuleYamlHandler.toYaml(notificationRule, appId))
            .collect(Collectors.toList());

    yaml.setDescription(workflow.getDescription());
    yaml.setEnvName(envName);
    yaml.setTemplateExpressions(templateExprYamlList);
    yaml.setTemplatized(workflow.isTemplatized());
    yaml.setType(orchestrationWorkflow.getOrchestrationWorkflowType().name());
    yaml.setPhases(phaseYamlList);
    yaml.setRollbackPhases(rollbackPhaseYamlList);
    yaml.setUserVariables(variableYamlList);
    yaml.setPreDeploymentSteps(preDeployStepsYamlList);
    yaml.setPostDeploymentSteps(postDeployStepsYamlList);
    yaml.setNotificationRules(notificationRuleYamlList);
    yaml.setFailureStrategies(failureStrategyYamlList);
    yaml.setHarnessApiVersion(getHarnessApiVersion());
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
    return yamlHelper.getWorkflow(accountId, yamlFilePath);
  }

  @Override
  public void delete(ChangeContext<Y> changeContext) throws HarnessException {
    Workflow workflow = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    if (workflow != null) {
      workflowService.deleteWorkflow(workflow.getAppId(), workflow.getUuid());
    }
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
