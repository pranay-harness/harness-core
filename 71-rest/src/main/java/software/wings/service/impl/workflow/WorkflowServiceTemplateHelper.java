package software.wings.service.impl.workflow;

import static io.harness.beans.OrchestrationWorkflowType.BASIC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.exception.InvalidRequestException;
import software.wings.api.DeploymentType;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateHelper;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.service.intfc.template.TemplateService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

@Singleton
public class WorkflowServiceTemplateHelper {
  private static final String ENV_VAR_DESC = "Variable for Environment entity";
  private static final String SERVICE_VAR_DESC = "Variable for Service entity";
  private static final String SERVICE_INFRA_VAR_DESC = "Variable for Service Infra-structure entity";
  private static final String INFRADEF_VAR_DESC = "Variable for Infrastructure Definition entity";
  private static final String APPD_SERVER_VAR_DESC = "Variable for AppDynamics Server entity";
  private static final String APPD_APP_VAR_DESC = "Variable for AppDynamics Application entity";
  private static final String APPD_TIER_VAR_DESC = "Variable for AppDynamics Tier entity";
  private static final String ELK_SERVER_VAR_DESC = "Variable for Elastic Search Server entity";
  private static final String ELK_INDICES_VAR_DESC = "Variable for Elastic Search Indices entity";
  private static final String CF_AWSCONFIG_VAR_DESC = "Variable for CloudFormation AWS Config entity";
  private static final String HELM_GITCONFIG_VAR_DESC = "Variable for Helm Git Config entity";

  @Inject private TemplateService templateService;
  @Inject private TemplateHelper templateHelper;

  public void updateLinkedPhaseStepTemplate(PhaseStep phaseStep, PhaseStep oldPhaseStep) {
    updateLinkedPhaseStepTemplate(phaseStep, oldPhaseStep, false);
  }

  public void populatePropertiesFromWorkflow(Workflow workflow) {
    if (!(workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow)) {
      return;
    }
    // load Linked Pre-deployment steps
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    populatePropertiesFromWorkflow(canaryOrchestrationWorkflow.getPreDeploymentSteps());

    // load Linked Post-deployment steps
    populatePropertiesFromWorkflow(canaryOrchestrationWorkflow.getPostDeploymentSteps());

    // Update Workflow Phase steps
    if (isNotEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
      for (WorkflowPhase workflowPhase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
        List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
        if (isNotEmpty(phaseSteps)) {
          for (PhaseStep phaseStep : phaseSteps) {
            populatePropertiesFromWorkflow(phaseStep);
          }
        }
      }
    }

    // Update Rollback Phase Steps
    if (canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap() != null) {
      canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().values().forEach(workflowPhase -> {
        if (isNotEmpty(workflowPhase.getPhaseSteps())) {
          for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
            populatePropertiesFromWorkflow(phaseStep);
          }
        }
      });
    }
  }

  private void populatePropertiesFromWorkflow(PhaseStep phaseStep) {
    if (phaseStep != null && phaseStep.getSteps() != null) {
      for (GraphNode step : phaseStep.getSteps()) {
        if (step.getTemplateUuid() != null) {
          Template template = templateService.get(step.getTemplateUuid(), step.getTemplateVersion());
          notNullCheck("Template does not exist", template, USER);
          GraphNode templateStep =
              (GraphNode) templateService.constructEntityFromTemplate(template, EntityType.WORKFLOW);
          List<String> templateProperties = templateService.fetchTemplateProperties(template);
          if (templateProperties != null) {
            if (!"COMMAND".equals(step.getType())) {
              step.getProperties().keySet().removeAll(templateProperties);
              step.getProperties().putAll(templateStep.getProperties());
            }
          }
        }
      }
    }
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
          Template template = templateService.get(step.getTemplateUuid(), step.getTemplateVersion());
          notNullCheck("Template does not exist", template, USER);
          GraphNode templateStep =
              (GraphNode) templateService.constructEntityFromTemplate(template, EntityType.WORKFLOW);
          step.setTemplateVariables(
              templateHelper.overrideVariables(templateStep.getTemplateVariables(), oldTemplateVariables));
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
                              && phaseStep.getPhaseStepType() == phaseStep1.getPhaseStepType()
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
    TemplateExpression infraMappingExpression = getTemplateExpression(sourceTemplateExpressions, "infraMappingId");
    TemplateExpression infraDefinitionExpression =
        getTemplateExpression(sourceTemplateExpressions, "infraDefinitionId");
    if (serviceExpression != null) {
      targetTemplateExpressions.add(serviceExpression);
    }
    if (infraMappingExpression != null) {
      targetTemplateExpressions.add(infraMappingExpression);
    }
    if (infraDefinitionExpression != null) {
      targetTemplateExpressions.add(infraDefinitionExpression);
    }
  }

  /**
   * Set template expressions to phase from workflow level
   *
   * @param workflow
   * @param workflowPhase
   */
  public static void setTemplateExpresssionsFromPhase(
      Workflow workflow, WorkflowPhase workflowPhase, boolean infraRefactor) {
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
      if (infraRefactor) {
        validateTemplateExpressionsInfraRefactor(templateExpressions);
      } else {
        validateTemplateExpressions(templateExpressions);
      }

      workflow.setTemplateExpressions(templateExpressions);
    }
  }

  public static void validateTemplateExpressions(List<TemplateExpression> templateExpressions) {
    // Validate combinations
    TemplateExpression envExpression =
        WorkflowServiceTemplateHelper.getTemplateExpression(templateExpressions, "envId");
    TemplateExpression serviceExpression = getTemplateExpression(templateExpressions, "serviceId");
    TemplateExpression infraMappingExpression = getTemplateExpression(templateExpressions, "infraMappingId");

    // It means nullifying both Service and InfraMappings .. throw an error if environment is templatized
    // Infra not present
    if (envExpression != null) {
      if (infraMappingExpression == null) {
        throw new InvalidRequestException(
            "Service Infrastructure cannot be de-templatized because Environment is templatized", USER);
      }
    }
    // Infra not present
    if (serviceExpression != null) {
      if (infraMappingExpression == null) {
        throw new InvalidRequestException(
            "Service Infrastructure cannot be de-templatized because Service is templatized", USER);
      }
    }
  }

  public static void validateTemplateExpressionsInfraRefactor(List<TemplateExpression> templateExpressions) {
    // Validate combinations
    TemplateExpression envExpression =
        WorkflowServiceTemplateHelper.getTemplateExpression(templateExpressions, "envId");
    TemplateExpression infraExpression = getTemplateExpression(templateExpressions, "infraDefinitionId");

    // It means nullifying both Service and InfraMappings .. throw an error if environment is templatized
    // Infra not present
    if (envExpression != null) {
      if (infraExpression == null) {
        throw new InvalidRequestException(
            "Infra Definition cannot be de-templatized because Environment is templatized", USER);
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
          .filter((Variable variable) -> ENVIRONMENT == variable.obtainEntityType())
          .map(Variable::getName)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  public static Variable getEnvVariable(List<Variable> variables) {
    if (isNotEmpty(variables)) {
      return variables.stream()
          .filter((Variable variable) -> ENVIRONMENT == variable.obtainEntityType())
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

  /***
   * Templatizes the service infra if environment templatized for Phase
   */
  public static void templatizeInfraDefinition(OrchestrationWorkflow orchestrationWorkflow, WorkflowPhase workflowPhase,
      List<TemplateExpression> phaseTemplateExpressions, Service service) {
    if (isEmpty(orchestrationWorkflow.getUserVariables())) {
      return;
    }
    List<String> infraDefinitionVariables =
        getInfraDefinitionWorkflowVariables(orchestrationWorkflow.getUserVariables());

    Map<String, Object> metaData = new HashMap<>();
    metaData.put(Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION.name());
    if (service.getArtifactType() != null) {
      metaData.put(Variable.ARTIFACT_TYPE, service.getArtifactType().name());
    }
    String expression = "${InfraDefinition";
    int i = 1;
    for (String infraDefinitionVariable : infraDefinitionVariables) {
      if (infraDefinitionVariable.startsWith("InfraDefinition")) {
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
        TemplateExpression.builder().fieldName("infraDefinitionId").metadata(metaData).expression(expression).build());
    orchestrationWorkflow.addToUserVariables(
        phaseTemplateExpressions, StateType.PHASE.name(), workflowPhase.getName(), null);
  }

  public static String getInfraDefExpressionFromInfraMappingExpression(String infraMappingExpression) {
    String infraDefExpression;
    if (infraMappingExpression.startsWith("${ServiceInfra")) {
      return infraMappingExpression.replace("ServiceInfra", "InfraDefinition");
    }
    return infraMappingExpression;
  }

  public static String getInfraDefVariableNameFromInfraMappingVariableName(String variableName) {
    String infraDefExpression;
    if (variableName.startsWith("ServiceInfra")) {
      return variableName.replace("ServiceInfra", "InfraDefinition");
    }
    return variableName;
  }

  public static List<String> getServiceInfrastructureWorkflowVariables(List<Variable> variables) {
    if (isEmpty(variables)) {
      return new ArrayList<>();
    }
    return variables.stream()
        .filter(
            variable -> variable.obtainEntityType() != null && variable.obtainEntityType() == INFRASTRUCTURE_MAPPING)
        .map(Variable::getName)
        .distinct()
        .collect(toList());
  }

  public static List<String> getInfraDefinitionWorkflowVariables(List<Variable> variables) {
    if (isEmpty(variables)) {
      return new ArrayList<>();
    }
    return variables.stream()
        .filter(
            variable -> variable.obtainEntityType() != null && variable.obtainEntityType() == INFRASTRUCTURE_DEFINITION)
        .map(Variable::getName)
        .distinct()
        .collect(toList());
  }

  public static List<Variable> getInfraDefCompleteWorkflowVariables(List<Variable> variables) {
    if (isEmpty(variables)) {
      return new ArrayList<>();
    }
    return variables.stream()
        .filter(
            variable -> variable.obtainEntityType() != null && variable.obtainEntityType() == INFRASTRUCTURE_DEFINITION)
        .distinct()
        .collect(toList());
  }

  public static List<String> getServiceWorkflowVariables(List<Variable> variables) {
    if (isEmpty(variables)) {
      return new ArrayList<>();
    }
    return variables.stream()
        .filter(variable -> variable.obtainEntityType() != null && variable.obtainEntityType() == SERVICE)
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

  public static boolean isInfraDefinitionTemplatized(List<TemplateExpression> templateExpressions) {
    if (templateExpressions == null) {
      return false;
    }
    return templateExpressions.stream().anyMatch(
        templateExpression -> templateExpression.getFieldName().equals("infraDefinitionId"));
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

  public static String getName(String expression) {
    return ManagerExpressionEvaluator.getName(expression);
  }

  public static String validateAndGetVariable(String variable, EntityType entityType) {
    if (variable.startsWith("workflow.variables.")) {
      variable = variable.replace("workflow.variables.", "");
    }
    Matcher matcher = ManagerExpressionEvaluator.variableNamePattern.matcher(variable);
    if (entityType != null) {
      if (!matcher.matches()) {
        throw new InvalidRequestException("Template variable:[" + variable + "] contains special characters", USER);
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
        case INFRASTRUCTURE_DEFINITION:
          return BASIC == orchestrationWorkflowType ? INFRADEF_VAR_DESC : INFRADEF_VAR_DESC + " in " + stateName;
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
