package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.FailureStrategy;
import software.wings.beans.GraphNode;
import software.wings.beans.NotificationRule;
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
import software.wings.common.Constants;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.notification.NotificationRulesYamlHandler;
import software.wings.service.impl.yaml.handler.template.TemplateExpressionYamlHandler;
import software.wings.service.impl.yaml.handler.variable.VariableYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.WorkflowService;
import software.wings.yaml.workflow.StepYaml;
import software.wings.yaml.workflow.WorkflowYaml;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    Workflow previous = get(accountId, yamlFilePath);

    WorkflowBuilder workflowBuilder = WorkflowBuilder.aWorkflow();
    toBean(changeContext, changeSetContext, workflowBuilder, previous);

    workflowBuilder.withSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      previous.setSyncFromGit(changeContext.getChange().isSyncFromGit());
      workflowBuilder.withUuid(previous.getUuid());
      return workflowService.updateLinkedWorkflow(workflowBuilder.build(), previous);
    } else {
      return workflowService.createWorkflow(workflowBuilder.build());
    }
  }

  private void toBean(ChangeContext<Y> changeContext, List<ChangeContext> changeContextList, WorkflowBuilder workflow,
      Workflow previous) throws HarnessException {
    WorkflowYaml yaml = changeContext.getYaml();
    Change change = changeContext.getChange();

    String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());
    notNullCheck("Could not locate app info in file path:" + change.getFilePath(), appId, USER);

    // Environment can be null in cloned workflows
    Environment environment = environmentService.getEnvironmentByName(appId, yaml.getEnvName());

    String envId = environment != null ? environment.getUuid() : null;

    try {
      WorkflowPhaseYamlHandler phaseYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.PHASE);

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
                            String workflowPhaseId = getPreviousWorkflowPhaseId(workflowPhase.getName(), previous);
                            if (workflowPhaseId != null) {
                              workflowPhase.setUuid(workflowPhaseId);
                            }
                            return workflowPhase;
                          } catch (HarnessException e) {
                            throw new WingsException(e);
                          }
                        })
                        .collect(toList());
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
                    String workflowPhaseId = getPreviousWorkflowPhaseId(workflowPhase.getName(), previous);
                    if (workflowPhaseId != null) {
                      workflowPhase.setUuid(workflowPhaseId);
                    }

                    return workflowPhase;
                  } catch (HarnessException e) {
                    throw new WingsException(e);
                  }
                })
                .collect(toList());
        rollbackPhaseList.forEach(rollbackPhase -> {
          WorkflowPhase workflowPhase = workflowPhaseMap.get(rollbackPhase.getPhaseNameForRollback());
          if (workflowPhase != null) {
            rollbackPhaseMap.put(workflowPhase.getUuid(), rollbackPhase);
          }
        });
      }

      // user variables
      List<Variable> userVariables = Lists.newArrayList();
      if (yaml.getUserVariables() != null) {
        VariableYamlHandler variableYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.VARIABLE);
        userVariables = yaml.getUserVariables()
                            .stream()
                            .map(userVariable -> {
                              try {
                                ChangeContext.Builder clonedContext =
                                    cloneFileChangeContext(changeContext, userVariable);
                                return variableYamlHandler.upsertFromYaml(clonedContext.build(), changeContextList);
                              } catch (HarnessException e) {
                                throw new WingsException(e);
                              }
                            })
                            .collect(toList());
      }

      // template expressions
      List<TemplateExpression> templateExpressions = Lists.newArrayList();
      if (yaml.getTemplateExpressions() != null) {
        TemplateExpressionYamlHandler templateExprYamlHandler =
            yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION);

        templateExpressions =
            yaml.getTemplateExpressions()
                .stream()
                .map(templateExpr -> {
                  try {
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, templateExpr);
                    return templateExprYamlHandler.upsertFromYaml(clonedContext.build(), changeContextList);
                  } catch (HarnessException e) {
                    throw new WingsException(e);
                  }
                })
                .collect(toList());
      }

      StepYamlHandler stepYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.STEP);

      // Pre-deployment steps
      PhaseStepBuilder preDeploymentSteps =
          PhaseStepBuilder.aPhaseStep(PhaseStepType.PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT);

      if (yaml.getPreDeploymentSteps() != null) {
        List<GraphNode> stepList =
            yaml.getPreDeploymentSteps()
                .stream()
                .map(stepYaml -> {
                  try {
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, stepYaml);
                    return stepYamlHandler.upsertFromYaml(clonedContext.build(), changeContextList);
                  } catch (HarnessException e) {
                    throw new WingsException(e);
                  }
                })
                .collect(toList());
        preDeploymentSteps.addAllSteps(stepList).build();
      }

      // Post-deployment steps
      PhaseStepBuilder postDeploymentSteps =
          PhaseStepBuilder.aPhaseStep(PhaseStepType.POST_DEPLOYMENT, Constants.POST_DEPLOYMENT);

      if (yaml.getPostDeploymentSteps() != null) {
        List<GraphNode> postDeployStepList =
            yaml.getPostDeploymentSteps()
                .stream()
                .map(stepYaml -> {
                  try {
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, stepYaml);
                    return stepYamlHandler.upsertFromYaml(clonedContext.build(), changeContextList);
                  } catch (HarnessException e) {
                    throw new WingsException(e);
                  }
                })
                .collect(toList());
        postDeploymentSteps.addAllSteps(postDeployStepList).build();
      }

      // Failure strategies
      List<FailureStrategy> failureStrategies = Lists.newArrayList();
      if (yaml.getFailureStrategies() != null) {
        FailureStrategyYamlHandler failureStrategyYamlHandler =
            yamlHandlerFactory.getYamlHandler(YamlType.FAILURE_STRATEGY);
        failureStrategies =
            yaml.getFailureStrategies()
                .stream()
                .map(failureStrategy -> {
                  try {
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, failureStrategy);
                    return failureStrategyYamlHandler.upsertFromYaml(clonedContext.build(), changeContextList);
                  } catch (HarnessException e) {
                    throw new WingsException(e);
                  }
                })
                .collect(toList());
      }

      // Notification rules
      List<NotificationRule> notificationRules = Lists.newArrayList();
      if (yaml.getNotificationRules() != null) {
        NotificationRulesYamlHandler notificationRuleYamlHandler =
            yamlHandlerFactory.getYamlHandler(YamlType.NOTIFICATION_RULE);
        notificationRules =
            yaml.getNotificationRules()
                .stream()
                .map(notificationRule -> {
                  try {
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, notificationRule);
                    return notificationRuleYamlHandler.upsertFromYaml(clonedContext.build(), changeContextList);
                  } catch (HarnessException e) {
                    throw new WingsException(e);
                  }
                })
                .collect(toList());
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

  private String getPreviousWorkflowPhaseId(String name, Workflow previous) {
    if (previous == null || previous.getOrchestrationWorkflow() == null) {
      return null;
    }

    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) previous.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhaseList = orchestrationWorkflow.getWorkflowPhases()
                                                .stream()
                                                .filter(phase -> name.equals(phase.getName()))
                                                .collect(Collectors.toList());
    int size = workflowPhaseList.size();
    // If size is greater than, we don't know what to pick, so we return null.
    if (size == 1) {
      return workflowPhaseList.get(0).getUuid();
    } else {
      return null;
    }
  }

  protected void toYaml(Y yaml, Workflow workflow, String appId) {
    // Environment can be null in case of incomplete cloned workflows
    String envName = null;
    if (isNotBlank(workflow.getEnvId())) {
      Environment environment = environmentService.get(appId, workflow.getEnvId());
      envName = environment != null ? environment.getName() : null;
    }

    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhases = orchestrationWorkflow.getWorkflowPhases();

    // phases
    WorkflowPhaseYamlHandler phaseYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.PHASE);
    List<WorkflowPhase.Yaml> phaseYamlList =
        workflowPhases.stream().map(workflowPhase -> phaseYamlHandler.toYaml(workflowPhase, appId)).collect(toList());

    // rollback phases
    Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = orchestrationWorkflow.getRollbackWorkflowPhaseIdMap();
    List<WorkflowPhase.Yaml> rollbackPhaseYamlList = Lists.newArrayList();
    orchestrationWorkflow.getWorkflowPhaseIds().forEach(workflowPhaseId -> {
      WorkflowPhase rollbackPhase = rollbackWorkflowPhaseIdMap.get(workflowPhaseId);
      if (rollbackPhase != null) {
        Yaml rollbackPhaseYaml = phaseYamlHandler.toYaml(rollbackPhase, appId);
        rollbackPhaseYamlList.add(rollbackPhaseYaml);
      }
    });

    // user variables
    List<Variable> userVariables = orchestrationWorkflow.getUserVariables();
    VariableYamlHandler variableYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.VARIABLE);
    List<Variable.Yaml> variableYamlList =
        userVariables.stream().map(userVariable -> variableYamlHandler.toYaml(userVariable, appId)).collect(toList());

    // template expressions
    TemplateExpressionYamlHandler templateExpressionYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION);
    List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();
    List<TemplateExpression.Yaml> templateExprYamlList = null;
    if (templateExpressions != null) {
      templateExprYamlList =
          templateExpressions.stream()
              .map(templateExpression -> templateExpressionYamlHandler.toYaml(templateExpression, appId))
              .collect(toList());
    }

    StepYamlHandler stepYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.STEP);
    // Pre-deployment steps
    PhaseStep preDeploymentSteps = orchestrationWorkflow.getPreDeploymentSteps();
    List<StepYaml> preDeployStepsYamlList =
        preDeploymentSteps.getSteps().stream().map(step -> stepYamlHandler.toYaml(step, appId)).collect(toList());

    // Post-deployment steps
    PhaseStep postDeploymentSteps = orchestrationWorkflow.getPostDeploymentSteps();
    List<StepYaml> postDeployStepsYamlList =
        postDeploymentSteps.getSteps().stream().map(step -> stepYamlHandler.toYaml(step, appId)).collect(toList());

    // Failure strategies
    FailureStrategyYamlHandler failureStrategyYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.FAILURE_STRATEGY);
    List<FailureStrategy> failureStrategies = orchestrationWorkflow.getFailureStrategies();
    List<FailureStrategy.Yaml> failureStrategyYamlList =
        failureStrategies.stream()
            .map(failureStrategy -> failureStrategyYamlHandler.toYaml(failureStrategy, appId))
            .collect(toList());

    // Notification rules
    NotificationRulesYamlHandler notificationRuleYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.NOTIFICATION_RULE);
    List<NotificationRule> notificationRules = orchestrationWorkflow.getNotificationRules();
    List<NotificationRule.Yaml> notificationRuleYamlList =
        notificationRules.stream()
            .map(notificationRule -> notificationRuleYamlHandler.toYaml(notificationRule, appId))
            .collect(toList());

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
  public Class getYamlClass() {
    return WorkflowYaml.class;
  }

  @Override
  public Workflow get(String accountId, String yamlFilePath) {
    return yamlHelper.getWorkflow(accountId, yamlFilePath);
  }

  @Override
  public void delete(ChangeContext<Y> changeContext) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, yamlFilePath);
    if (!optionalApplication.isPresent()) {
      return;
    }

    Workflow workflow = yamlHelper.getWorkflowByAppIdYamlPath(optionalApplication.get().getUuid(), yamlFilePath);
    if (workflow != null) {
      workflowService.deleteByYamlGit(
          workflow.getAppId(), workflow.getUuid(), changeContext.getChange().isSyncFromGit());
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
