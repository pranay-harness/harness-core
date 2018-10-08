package software.wings.service.impl.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.api.DeploymentType.AMI;
import static software.wings.api.DeploymentType.AWS_CODEDEPLOY;
import static software.wings.api.DeploymentType.AWS_LAMBDA;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.OrchestrationWorkflowType.BASIC;
import static software.wings.common.Constants.APPD_APP_VAR_DESC;
import static software.wings.common.Constants.APPD_SERVER_VAR_DESC;
import static software.wings.common.Constants.APPD_TIER_VAR_DESC;
import static software.wings.common.Constants.ARTIFACT_TYPE;
import static software.wings.common.Constants.ELK_INDICES_VAR_DESC;
import static software.wings.common.Constants.ELK_SERVER_VAR_DESC;
import static software.wings.common.Constants.ENTITY_TYPE;
import static software.wings.common.Constants.ENV_VAR_DESC;
import static software.wings.common.Constants.SERVICE_INFRA_VAR_DESC;
import static software.wings.common.Constants.SERVICE_VAR_DESC;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.api.DeploymentType;
import software.wings.beans.EntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflowType;
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
    if (phaseStep != null && phaseStep.getSteps() != null) {
      phaseStep.getSteps().stream().filter(step -> step.getTemplateUuid() != null).forEach((GraphNode step) -> {
        GraphNode oldTemplateStep = null;
        if (oldPhaseStep != null && oldPhaseStep.getSteps() != null) {
          oldTemplateStep = oldPhaseStep.getSteps()
                                .stream()
                                .filter(graphNode -> step.getTemplateUuid().equals(graphNode.getTemplateUuid()))
                                .findFirst()
                                .orElse(null);
        }
        boolean versionChanged = false;
        List<Variable> oldTemplateVaraibles = null;
        if (oldTemplateStep != null) {
          if (step.getTemplateVersion() != null && oldTemplateStep.getTemplateVersion() != null
              && !step.getTemplateVersion().equals(oldTemplateStep.getTemplateVersion())) {
            versionChanged = true;
          }
          oldTemplateVaraibles = oldTemplateStep.getTemplateVariables();
        }
        if (versionChanged || oldTemplateStep == null) {
          GraphNode templateStep = (GraphNode) templateService.constructEntityFromTemplate(
              step.getTemplateUuid(), step.getTemplateVersion());
          Validator.notNullCheck("Template does not exist", templateStep, USER);
          step.setTemplateVariables(templateStep.getTemplateVariables());
          templateHelper.updateVariables(step.getTemplateVariables(), oldTemplateVaraibles, true);
          if (step.getProperties() != null) {
            if (templateStep.getProperties() != null) {
              step.getProperties().putAll(templateStep.getProperties());
            }
          }
        }
      });
    }
  }

  public void updateLinkedWorkflowPhases(
      List<WorkflowPhase> workflowPhases, List<WorkflowPhase> existingWorkflowPhases) {
    for (WorkflowPhase workflowPhase : workflowPhases) {
      WorkflowPhase oldWorkflowPhase = existingWorkflowPhases == null
          ? null
          : existingWorkflowPhases.stream()
                .filter(existingWorkflowPhase -> workflowPhase.getUuid().equals(existingWorkflowPhase.getUuid()))
                .findFirst()
                .orElse(null);
      updateLinkedWorkflowPhaseTemplate(workflowPhase, oldWorkflowPhase);
    }
  }

  public void updateLinkedWorkflowPhaseTemplate(WorkflowPhase workflowPhase, WorkflowPhase oldWorkflowPhase) {
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
                      .filter(
                          phaseStep1 -> phaseStep.getUuid() != null && phaseStep.getUuid().equals(phaseStep1.getUuid()))
                      .findFirst()
                      .orElse(null);
              updateLinkedPhaseStepTemplate(phaseStep, oldPhaseStep);
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
    if (SSH.equals(deploymentType)) {
      expression = expression + "_SSH";
    } else if (AWS_CODEDEPLOY.equals(deploymentType)) {
      expression = expression + "_AWS_CodeDeploy";
    } else if (ECS.equals(deploymentType)) {
      expression = expression + "_ECS";
    } else if (KUBERNETES.equals(deploymentType)) {
      expression = expression + "_Kubernetes";
    } else if (AWS_LAMBDA.equals(deploymentType)) {
      expression = expression + "_AWS_Lambda";
    } else if (AMI.equals(deploymentType)) {
      expression = expression + "_AMI";
    }
    return expression;
  }

  public static String getTemplatizedEnvVariableName(List<Variable> variables) {
    if (isNotEmpty(variables)) {
      return variables.stream()
          .filter((Variable variable) -> ENVIRONMENT.equals(variable.getEntityType()))
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
    metaData.put(ENTITY_TYPE, INFRASTRUCTURE_MAPPING.name());
    if (service.getArtifactType() != null) {
      metaData.put(ARTIFACT_TYPE, service.getArtifactType().name());
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
        .filter(variable -> variable.getEntityType() != null && variable.getEntityType().equals(INFRASTRUCTURE_MAPPING))
        .map(Variable::getName)
        .distinct()
        .collect(toList());
  }

  public static List<String> getServiceWorkflowVariables(List<Variable> variables) {
    if (isEmpty(variables)) {
      return new ArrayList<>();
    }
    return variables.stream()
        .filter(variable -> variable.getEntityType() != null && variable.getEntityType().equals(SERVICE))
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
      default:
        return "";
    }
  }
}
