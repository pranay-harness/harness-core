package software.wings.service.impl.trigger;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.Action.ActionType;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.Validator;

import java.util.List;

@Singleton
public class TriggerAuthHandler {
  @Inject AuthHandler authHandler;
  @Inject EnvironmentService environmentService;
  @Inject AuthService authService;
  @Inject PipelineService pipelineService;
  @Inject WorkflowService workflowService;

  public void authorize(DeploymentTrigger trigger, boolean existing) {
    if (trigger.getAction() == null) {
      return;
    }

    boolean envParamaterized;
    List<Variable> variables;
    List<Variable> triggerVariables = null;
    ActionType actionType = trigger.getAction().getActionType();
    switch (actionType) {
      case PIPELINE:
        PipelineAction pipelineAction = (PipelineAction) trigger.getAction();
        String pipeLineId = pipelineAction.getPipelineId();
        try {
          authorizeWorkflowOrPipeline(trigger.getAppId(), pipeLineId);
        } catch (WingsException ex) {
          throw new WingsException(
              "User does not have deployment execution permission on Pipeline: " + pipeLineId, USER);
        }
        Pipeline pipeline = pipelineService.readPipeline(trigger.getAppId(), pipeLineId, true);
        Validator.notNullCheck("Pipeline does not exist", pipeline, USER);
        envParamaterized = pipeline.isEnvParameterized();
        variables = pipeline.getPipelineVariables();
        if (pipelineAction.getTriggerArgs() != null) {
          triggerVariables = pipelineAction.getTriggerArgs().getVariables();
        }
        break;
      case ORCHESTRATION:
        WorkflowAction workflowAction = (WorkflowAction) trigger.getAction();
        String workflowId = workflowAction.getWorkflowId();
        try {
          authorizeWorkflowOrPipeline(trigger.getAppId(), workflowId);
        } catch (WingsException ex) {
          throw new WingsException(
              "User does not have deployment execution permission on Workflow: " + workflowId, USER);
        }
        Workflow workflow = workflowService.readWorkflow(trigger.getAppId(), workflowId);
        Validator.notNullCheck("Workflow does not exist", workflow, USER);
        Validator.notNullCheck("Orchestration workflow does not exist", workflow.getOrchestrationWorkflow(), USER);
        envParamaterized = workflow.checkEnvironmentTemplatized();
        variables = workflow.getOrchestrationWorkflow().getUserVariables();
        if (workflowAction.getTriggerArgs() != null) {
          triggerVariables = workflowAction.getTriggerArgs().getVariables();
        }
        break;
      default:
        throw new WingsException("Action Type not supported: " + actionType, USER);
    }

    if (envParamaterized) {
      if (triggerVariables == null) {
        if (existing) {
          return;
        }
        throw new WingsException("Please select a value for entity type variables.", USER);
      }

      String templatizedEnvVariableName = WorkflowServiceTemplateHelper.getTemplatizedEnvVariableName(variables);
      if (isEmpty(templatizedEnvVariableName)) {
        return;
      }
      validateAndAuthorizeEnvironment(trigger, existing, triggerVariables);
    }
  }

  private void validateAndAuthorizeEnvironment(
      DeploymentTrigger trigger, boolean existing, List<Variable> triggerVariables) {
    Variable environmentVaribale = WorkflowServiceTemplateHelper.getEnvVariable(triggerVariables);
    if (environmentVaribale == null || isEmpty(environmentVaribale.getValue())) {
      if (existing) {
        return;
      }
      throw new WingsException("Environment is parameterized. Please select a value in the format ${varName}.", USER);
    }
    authorizeEnvironment(trigger.getAppId(), environmentVaribale.getValue());
  }

  public void authorizeEnvironment(String appId, String environmentValue) {
    if (ManagerExpressionEvaluator.matchesVariablePattern(environmentValue)) {
      try {
        authHandler.authorizeAccountPermission(
            asList(new PermissionAttribute(PermissionType.ACCOUNT_MANAGEMENT, Action.READ)));
      } catch (WingsException ex) {
        throw new WingsException(
            "User not authorized: Only members of the Account Administrator user group can create or update Triggers with parameterized variables.",
            USER);
      }
    } else {
      // Check if environment exist by envId
      Environment environment = environmentService.get(appId, environmentValue);
      if (environment != null) {
        try {
          authService.checkIfUserAllowedToDeployToEnv(appId, environmentValue);
        } catch (WingsException ex) {
          throw new WingsException(
              "User does not have deployment execution permission on environment. [" + environment.getName() + "]",
              USER);
        }

      } else {
        // either environment does not exist or user give some random name.. then check account level permission
        try {
          authHandler.authorizeAccountPermission(
              asList(new PermissionAttribute(PermissionType.ACCOUNT_MANAGEMENT, Action.READ)));
        } catch (WingsException ex) {
          throw new WingsException(
              "User not authorized: Only members of the Account Administrator user group can create or update Triggers with parameterized variables",
              USER);
        }
      }
    }
  }

  public void authorizeWorkflowOrPipeline(String appId, String workflowOrPipelineId) {
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.DEPLOYMENT, Action.EXECUTE);
    List<PermissionAttribute> permissionAttributeList = asList(permissionAttribute);
    authHandler.authorize(permissionAttributeList, asList(appId), workflowOrPipelineId);
  }
}
