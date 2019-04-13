package software.wings.service.impl.workflow;

import static io.harness.beans.OrchestrationWorkflowType.BASIC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.common.Constants.APPD_APP_VAR_DESC;
import static software.wings.common.Constants.APPD_SERVER_VAR_DESC;
import static software.wings.common.Constants.APPD_TIER_VAR_DESC;
import static software.wings.common.Constants.CF_AWSCONFIG_VAR_DESC;
import static software.wings.common.Constants.ELK_INDICES_VAR_DESC;
import static software.wings.common.Constants.ELK_SERVER_VAR_DESC;
import static software.wings.common.Constants.ENV_VAR_DESC;
import static software.wings.common.Constants.HELM_GITCONFIG_VAR_DESC;
import static software.wings.common.Constants.SERVICE_INFRA_VAR_DESC;
import static software.wings.common.Constants.SERVICE_VAR_DESC;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.api.DeploymentType;
import software.wings.beans.EntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.template.TemplateHelper;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.service.intfc.template.TemplateService;
import software.wings.sm.StateType;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

@Singleton
public class WorkflowServiceTemplateHelper {
  @Inject private TemplateService templateService;
  @Inject private TemplateHelper templateHelper;

  public void updateLinkedPhaseStepTemplate(PhaseStep phaseStep, PhaseStep oldPhaseStep) {
    updateLinkedPhaseStepTemplate(phaseStep, oldPhaseStep, false);
  }

  public void updateLinkedPhaseStepTemplate(PhaseStep phaseStep, PhaseStep oldPhaseStep, boolean fromYaml) {
    if (phaseStep != null && phaseStep.getSteps() != null) {
      phaseStep.getSteps().stream().filter(step -> step.getTemplateUuid() != null).forEach((GraphNode step) -> {
        GraphNode oldTemplateStep = null;
        if (oldPhaseStep != null && oldPhaseStep.getSteps() != null) {
          oldTemplateStep = oldPhaseStep.getSteps()
                                .stream()
                                .filter(fromYaml ? graphNode
                                    -> step.getTemplateUuid().equals(graphNode.getTemplateUuid())
                                        && step.getName().equals(graphNode.getName())
                                                 : graphNode
                                    -> step.getTemplateUuid().equals(graphNode.getTemplateUuid())
                                        && step.getId().equals(graphNode.getId()))
                                .findFirst()
                                .orElse(null);
        }
        boolean versionChanged = false;
        List<Variable> oldTemplateVariables = null;
        if (oldTemplateStep != null) {
          if (step.getTemplateVersion() != null && oldTemplateStep.getTemplateVersion() != null
              && !step.getTemplateVersion().equals(oldTemplateStep.getTemplateVersion())) {
            versionChanged = true;
          }
          oldTemplateVariables = oldTemplateStep.getTemplateVariables();
        }
        if (versionChanged || oldTemplateStep == null) {
          GraphNode templateStep = (GraphNode) templateService.constructEntityFromTemplate(
              step.getTemplateUuid(), step.getTemplateVersion(), EntityType.WORKFLOW);
          Validator.notNullCheck("Template does not exist", templateStep, USER);
          step.setTemplateVariables(
              templateHelper.overrideVariables(templateStep.getTemplateVariables(), oldTemplateVariables));
          if (step.getProperties() != null) {
            if (templateStep.getProperties() != null) {
              List<String> templateProperties =
                  templateService.fetchTemplateProperties(step.getTemplateUuid(), step.getTemplateVersion());
              step.getProperties().keySet().removeAll(templateProperties);
              if (!step.getType().equals("COMMAND")) {
                step.getProperties().putAll(templateStep.getProperties());
              }
            }
          }
        } else if (oldTemplateStep != null) {
          // Do not change the template properties
          List<String> templateProperties =
              templateService.fetchTemplateProperties(step.getTemplateUuid(), step.getTemplateVersion());
          if (step.getProperties() != null) {
            step.getProperties().keySet().removeAll(templateProperties);
            if (!step.getType().equals("COMMAND")) {
              if (oldTemplateStep.getProperties() != null) {
                for (String s : templateProperties) {
                  step.getProperties().put(s, oldTemplateStep.getProperties().get(s));
                }
              }
            }
          }
        }
      });
    }
  }

  public void updateLinkedWorkflowPhases(
      List<WorkflowPhase> workflowPhases, List<WorkflowPhase> existingWorkflowPhases, boolean fromYaml) {
    for (WorkflowPhase workflowPhase : workflowPhases) {
      WorkflowPhase oldWorkflowPhase = existingWorkflowPhases == null
          ? null
          : existingWorkflowPhases.stream()
                .filter(existingWorkflowPhase -> workflowPhase.getUuid().equals(existingWorkflowPhase.getUuid()))
                .findFirst()
                .orElse(null);
      updateLinkedWorkflowPhaseTemplate(workflowPhase, oldWorkflowPhase, fromYaml);
    }
  }

  public void updateLinkedWorkflowPhaseTemplate(WorkflowPhase workflowPhase, WorkflowPhase oldWorkflowPhase) {
    updateLinkedWorkflowPhaseTemplate(workflowPhase, oldWorkflowPhase, false);
  }

  public void updateLinkedWorkflowPhaseTemplate(
      WorkflowPhase workflowPhase, WorkflowPhase oldWorkflowPhase, boolean fromYaml) {
    if (oldWorkflowPhase == null) {
      return;
    }
    if (workflowPhase != null) {
      if (workflowPhase.getPhaseSteps() != null) {
        workflowPhase.getPhaseSteps()
            .stream()
            .filter(phaseStep -> oldWorkflowPhase.getPhaseSteps() != null)
            .forEach((PhaseStep phaseStep) -> {
              PhaseStep oldPhaseStep =
                  oldWorkflowPhase.getPhaseSteps()
                      .stream()
                      .filter(fromYaml ? phaseStep1
                          -> phaseStep.getPhaseStepType() != null
                              && phaseStep.getPhaseStepType().equals(phaseStep1.getPhaseStepType())
                              && phaseStep.getName() != null && phaseStep.getName().equals(phaseStep1.getName())
                                       : phaseStep1
                          -> phaseStep.getUuid() != null && phaseStep.getUuid().equals(phaseStep1.getUuid()))
                      .findFirst()
                      .orElse(null);
              updateLinkedPhaseStepTemplate(phaseStep, oldPhaseStep, fromYaml);
            });
      }
    }
  }

  public void addLinkedWorkflowPhaseTemplate(WorkflowPhase workflowPhase) {
    if (workflowPhase != null) {
      if (workflowPhase.getPhaseSteps() != null) {
        workflowPhase.getPhaseSteps().forEach((PhaseStep phaseStep) -> updateLinkedPhaseStepTemplate(phaseStep, null));
      }
    }
  }

  /**
   * Set template expressions to phase from workflow level
   *
   * @param templateExpressions
   * @param workflowPhase
   */
  public static void setTemplateExpresssionsToPhase(
      List<TemplateExpression> templateExpressions, WorkflowPhase workflowPhase) {
    if (workflowPhase == null) {
      return;
    }

    List<TemplateExpression> phaseTemplateExpressions = new ArrayList<>();
    addTemplateExpressions(templateExpressions, phaseTemplateExpressions);
    workflowPhase.setTemplateExpressions(phaseTemplateExpressions);
  }

  private static void addTemplateExpressions(
      List<TemplateExpression> sourceTemplateExpressions, List<TemplateExpression> targetTemplateExpressions) {
    TemplateExpression serviceExpression = getTemplateExpression(sourceTemplateExpressions, "serviceId");
    TemplateExpression infraExpression = getTemplateExpression(sourceTemplateExpressions, "infraMappingId");
    if (serviceExpression != null) {
      targetTemplateExpressions.add(serviceExpression);
    }
    if (infraExpression != null) {
      targetTemplateExpressions.add(infraExpression);
    }
  }

  /**
   * Set template expressions to phase from workflow level
   *
   * @param workflow
   * @param workflowPhase
   */
  public static void setTemplateExpresssionsFromPhase(Workflow workflow, WorkflowPhase workflowPhase) {
    List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();
    TemplateExpression envExpression = getTemplateExpression(templateExpressions, "envId");
    // Reset template expressions
    templateExpressions = new ArrayList<>();
    if (envExpression != null) {
      templateExpressions.add(envExpression);
    }
    if (workflowPhase != null) {
      List<TemplateExpression> phaseTemplateExpressions = workflowPhase.getTemplateExpressions();
      if (isEmpty(phaseTemplateExpressions)) {
        phaseTemplateExpressions = new ArrayList<>();
      }
      // It means, user templatizing it from phase level
      addTemplateExpressions(phaseTemplateExpressions, templateExpressions);
      validateTemplateExpressions(templateExpressions);
      workflow.setTemplateExpressions(templateExpressions);
    }
  }

  public static void validateTemplateExpressions(List<TemplateExpression> templateExpressions) {
    // Validate combinations
    TemplateExpression envExpression =
        WorkflowServiceTemplateHelper.getTemplateExpression(templateExpressions, "envId");
    TemplateExpression serviceExpression = getTemplateExpression(templateExpressions, "serviceId");
    TemplateExpression infraExpression = getTemplateExpression(templateExpressions, "infraMappingId");

    // It means nullifying both Service and InfraMappings .. throw an error if environment is templatized
    // Infra not present
    if (envExpression != null) {
      if (infraExpression == null) {
        throw new InvalidRequestException(
            "Service Infrastructure cannot be de-templatized because Environment is templatized", USER);
      }
    }
    // Infra not present
    if (serviceExpression != null) {
      if (infraExpression == null) {
        throw new InvalidRequestException(
            "Service Infrastructure cannot be de-templatized because Service is templatized", USER);
      }
    }
  }

  public static TemplateExpression getTemplateExpression(
      List<TemplateExpression> templateExpressions, String fieldName) {
    return templateExpressions == null
        ? null
        : templateExpressions.stream()
              .filter(templateExpression -> templateExpression.getFieldName().equals(fieldName))
              .findAny()
              .orElse(null);
  }

  public static String getInframappingExpressionName(DeploymentType deploymentType, String expression) {
    switch (deploymentType) {
      case SSH:
        return expression + "_SSH";
      case KUBERNETES:
        return expression + "_Kubernetes";
      case ECS:
        return expression + "_ECS";
      case AWS_LAMBDA:
        return expression + "_AWS_Lambda";
      case AMI:
        return expression + "_AMI";
      case AWS_CODEDEPLOY:
        return expression + "_AWS_CodeDeploy";
      case HELM:
        return expression + "_HELM";
      case WINRM:
        return expression + "_WINRM";
      case PCF:
        return expression + "_PCF";
      default:
        unhandled(deploymentType);
    }
    return expression;
  }

  public static String getTemplatizedEnvVariableName(List<Variable> variables) {
    if (isNotEmpty(variables)) {
      return variables.stream()
          .filter((Variable variable) -> ENVIRONMENT.equals(variable.obtainEntityType()))
          .map(Variable::getName)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  /***
   * Templatizes the service infra if environment templatized for Phase
   */
  public static void templatizeServiceInfra(OrchestrationWorkflow orchestrationWorkflow, WorkflowPhase workflowPhase,
      List<TemplateExpression> phaseTemplateExpressions, Service service) {
    if (isEmpty(orchestrationWorkflow.getUserVariables())) {
      return;
    }
    List<String> serviceInfraVariables =
        getServiceInfrastructureWorkflowVariables(orchestrationWorkflow.getUserVariables());

    Map<String, Object> metaData = new HashMap<>();
    metaData.put(Variable.ENTITY_TYPE, INFRASTRUCTURE_MAPPING.name());
    if (service.getArtifactType() != null) {
      metaData.put(Variable.ARTIFACT_TYPE, service.getArtifactType().name());
    }
    String expression = "${ServiceInfra";
    int i = 1;
    for (String serviceInfraVariable : serviceInfraVariables) {
      if (serviceInfraVariable.startsWith("ServiceInfra")) {
        i++;
      }
    }
    expression =
        WorkflowServiceTemplateHelper.getInframappingExpressionName(workflowPhase.getDeploymentType(), expression);
    if (i != 1) {
      expression = expression + i;
    }
    expression = expression + "}";
    phaseTemplateExpressions.add(
        TemplateExpression.builder().fieldName("infraMappingId").metadata(metaData).expression(expression).build());
    orchestrationWorkflow.addToUserVariables(
        phaseTemplateExpressions, StateType.PHASE.name(), workflowPhase.getName(), null);
  }

  public static List<String> getServiceInfrastructureWorkflowVariables(List<Variable> variables) {
    if (isEmpty(variables)) {
      return new ArrayList<>();
    }
    return variables.stream()
        .filter(variable
            -> variable.obtainEntityType() != null && variable.obtainEntityType().equals(INFRASTRUCTURE_MAPPING))
        .map(Variable::getName)
        .distinct()
        .collect(toList());
  }

  public static List<String> getServiceWorkflowVariables(List<Variable> variables) {
    if (isEmpty(variables)) {
      return new ArrayList<>();
    }
    return variables.stream()
        .filter(variable -> variable.obtainEntityType() != null && variable.obtainEntityType().equals(SERVICE))
        .map(Variable::getName)
        .distinct()
        .collect(toList());
  }

  /***
   *
   * @param templateExpressions
   * @return
   */
  public static boolean isEnvironmentTemplatized(List<TemplateExpression> templateExpressions) {
    if (templateExpressions == null) {
      return false;
    }
    return templateExpressions.stream().anyMatch(
        templateExpression -> templateExpression.getFieldName().equals("envId"));
  }

  public static boolean isInfraTemplatized(List<TemplateExpression> templateExpressions) {
    if (templateExpressions == null) {
      return false;
    }
    return templateExpressions.stream().anyMatch(
        templateExpression -> templateExpression.getFieldName().equals("infraMappingId"));
  }

  public static void transformEnvTemplateExpressions(Workflow workflow, OrchestrationWorkflow orchestrationWorkflow) {
    if (isNotEmpty(workflow.getTemplateExpressions())) {
      Optional<TemplateExpression> envExpression =
          workflow.getTemplateExpressions()
              .stream()
              .filter(templateExpression -> templateExpression.getFieldName().equals("envId"))
              .findAny();
      if (envExpression.isPresent()) {
        orchestrationWorkflow.addToUserVariables(asList(envExpression.get()));
      }
    }
  }

  public static String getName(String expression, EntityType entityType) {
    return validatetAndGetVariable(ManagerExpressionEvaluator.getName(expression), entityType);
  }

  public static String validatetAndGetVariable(String variable, EntityType entityType) {
    if (variable.startsWith("workflow.variables.")) {
      variable = variable.replace("workflow.variables.", "");
    }
    Matcher matcher = ManagerExpressionEvaluator.variableNamePattern.matcher(variable);
    if (entityType != null) {
      if (!matcher.matches()) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args", "Template variable:[" + variable + "] contains special characters");
      }
    }
    return variable;
  }

  public static String getVariableDescription(
      EntityType entityType, OrchestrationWorkflowType orchestrationWorkflowType, String stateName) {
    if (entityType != null) {
      switch (entityType) {
        case ENVIRONMENT:
          return ENV_VAR_DESC;
        case SERVICE:
          return BASIC == orchestrationWorkflowType ? SERVICE_VAR_DESC : SERVICE_VAR_DESC + " in " + stateName;
        case INFRASTRUCTURE_MAPPING:
          return BASIC == orchestrationWorkflowType ? SERVICE_INFRA_VAR_DESC
                                                    : SERVICE_INFRA_VAR_DESC + " in " + stateName;
        case APPDYNAMICS_CONFIGID:
          return APPD_SERVER_VAR_DESC + " in " + stateName;
        case APPDYNAMICS_APPID:
          return APPD_APP_VAR_DESC + " in " + stateName;
        case APPDYNAMICS_TIERID:
          return APPD_TIER_VAR_DESC + " in " + stateName;
        case ELK_CONFIGID:
          return ELK_SERVER_VAR_DESC + " in " + stateName;
        case ELK_INDICES:
          return ELK_INDICES_VAR_DESC + " in " + stateName;
        case CF_AWS_CONFIG_ID:
          return CF_AWSCONFIG_VAR_DESC + " in " + stateName;
        case HELM_GIT_CONFIG_ID:
          return HELM_GITCONFIG_VAR_DESC + " in " + stateName;
        default:
          return "";
      }
    }
    return "";
  }
}
