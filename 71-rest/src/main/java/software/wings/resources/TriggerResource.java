package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.Arrays.asList;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.WorkflowType;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Variable;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookParameters;
import software.wings.beans.trigger.WebhookSource;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by sgurubelli on 10/26/17.
 */
@Api("triggers")
@Path("/triggers")
@Produces("application/json")
@Consumes("application/json")
@Slf4j
@Scope(APPLICATION)
public class TriggerResource {
  private TriggerService triggerService;
  private AuthHandler authHandler;
  private AuthService authService;
  private WorkflowService workflowService;
  private PipelineService pipelineService;
  private EnvironmentService environmentService;

  @Inject
  public TriggerResource(TriggerService triggerService, AuthHandler authHandler, AuthService authService,
      WorkflowService workflowService, PipelineService pipelineService, EnvironmentService environmentService) {
    this.triggerService = triggerService;
    this.authHandler = authHandler;
    this.authService = authService;
    this.workflowService = workflowService;
    this.pipelineService = pipelineService;
    this.environmentService = environmentService;
  }

  /**
   * @param appId
   * @param pageRequest
   * @return
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Trigger>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Trigger> pageRequest) {
    pageRequest.addFilter("appId", EQ, appId);
    return new RestResponse<>(triggerService.list(pageRequest));
  }

  /**
   * Gets the.
   *
   * @param appId    the app id
   * @param triggerId the stream id
   * @return the rest response
   */
  @GET
  @Path("{triggerId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Trigger> get(@QueryParam("appId") String appId, @PathParam("triggerId") String triggerId) {
    return new RestResponse<>(triggerService.get(appId, triggerId));
  }

  /**
   * Save rest response.
   *
   * @param appId   the app id
   * @param trigger the artifact stream
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Trigger> save(@QueryParam("appId") String appId, Trigger trigger) {
    Validator.notNullCheck("trigger", trigger);
    trigger.setAppId(appId);
    if (trigger.getUuid() != null) {
      Trigger existingTrigger = triggerService.get(appId, trigger.getUuid());
      if (existingTrigger == null) {
        throw new WingsException("Trigger does not exist", USER);
      }
      authorize(existingTrigger, true);
      authorize(trigger, false);
      return new RestResponse(triggerService.update(trigger));
    }
    authorize(trigger, false);
    return new RestResponse<>(triggerService.save(trigger));
  }

  private void authorizeEnvironment(String appId, String environmentValue) {
    if (ManagerExpressionEvaluator.matchesVariablePattern(environmentValue)) {
      try {
        authHandler.authorizeAccountPermission(
            asList(new PermissionAttribute(PermissionType.ACCOUNT_MANAGEMENT, Action.READ)));
      } catch (WingsException ex) {
        throw new WingsException(
            "User not authorized: Only admin can create or update the trigger with parameterized variables.");
      }
    } else {
      // Check if environment exist by envId
      Environment environment = environmentService.get(appId, environmentValue);
      if (environment != null) {
        try {
          authService.checkIfUserAllowedToDeployToEnv(appId, environmentValue);
        } catch (WingsException ex) {
          throw new WingsException(
              "User does not have execute permission for environment [" + environment.getName() + "]");
        }

      } else {
        // either environment does not exist or user give some random name.. then check account level permission
        try {
          authHandler.authorizeAccountPermission(
              asList(new PermissionAttribute(PermissionType.ACCOUNT_MANAGEMENT, Action.READ)));
        } catch (WingsException ex) {
          throw new WingsException(
              "User not authorized: Only admin can create or update the trigger with parameterized variables.");
        }
      }
    }
  }

  private void authorize(Trigger trigger, boolean existing) {
    authorizeWorkflowOrPipeline(trigger.getAppId(), trigger.getWorkflowId());
    boolean envParamaterized;
    List<Variable> variables;
    if (PIPELINE.equals(trigger.getWorkflowType())) {
      Pipeline pipeline = pipelineService.readPipeline(trigger.getAppId(), trigger.getWorkflowId(), true);
      Validator.notNullCheck("Pipeline does not exist", pipeline, USER);
      envParamaterized = pipeline.isEnvParameterized();
      variables = pipeline.getPipelineVariables();
    } else if (WorkflowType.ORCHESTRATION.equals(trigger.getWorkflowType())) {
      Workflow workflow = workflowService.readWorkflow(trigger.getAppId(), trigger.getWorkflowId());
      Validator.notNullCheck("Workflow does not exist", workflow, USER);
      Validator.notNullCheck("Orchestration workflow does not exist", workflow.getOrchestrationWorkflow(), USER);
      envParamaterized = workflow.checkEnvironmentTemplatized();
      variables = workflow.getOrchestrationWorkflow().getUserVariables();
    } else {
      logger.error("WorkflowType {} not supported", trigger.getWorkflowType());
      throw new WingsException("Workflow Type [" + trigger.getWorkflowType() + "] not supported", USER);
    }
    if (envParamaterized) {
      validateAndAuthorizeEnvironment(trigger, existing, variables);
    }
  }

  private void validateAndAuthorizeEnvironment(Trigger trigger, boolean existing, List<Variable> variables) {
    String templatizedEnvVariableName = WorkflowServiceTemplateHelper.getTemplatizedEnvVariableName(variables);
    if (isNotEmpty(templatizedEnvVariableName)) {
      Map<String, String> workflowVariables = trigger.getWorkflowVariables();
      if (isEmpty(workflowVariables)) {
        if (existing) {
          return;
        }
        throw new WingsException("Please select value for Entity Type variables", USER);
      }
      String environment = workflowVariables.get(templatizedEnvVariableName);
      if (isEmpty(environment)) {
        if (existing) {
          return;
        }
        throw new WingsException("Environment is parameterized. Please select value in the format ${varName}", USER);
      }
      authorizeEnvironment(trigger.getAppId(), environment);
    }
  }

  private void authorizeWorkflowOrPipeline(String appId, String workflowOrPipelineId) {
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.DEPLOYMENT, Action.EXECUTE);
    List<PermissionAttribute> permissionAttributeList = asList(permissionAttribute);
    authHandler.authorize(permissionAttributeList, asList(appId), workflowOrPipelineId);
  }

  /**
   * Update rest response.
   *
   * @param appId    the app id
   * @param triggerId the stream id
   * @param trigger  the artifact stream
   * @return the rest response
   */
  @PUT
  @Path("{triggerId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Trigger> update(
      @QueryParam("appId") String appId, @PathParam("triggerId") String triggerId, Trigger trigger) {
    trigger.setUuid(triggerId);
    trigger.setAppId(appId);
    Trigger existingTrigger = triggerService.get(appId, trigger.getUuid());
    if (existingTrigger == null) {
      throw new WingsException("Trigger doesn't exist", USER);
    }
    authorize(existingTrigger, true);
    authorize(trigger, false);
    return new RestResponse<>(triggerService.update(trigger));
  }

  /**
   * Delete.
   *
   * @param appId the app id
   * @param triggerId    the id
   * @return the rest response
   */
  @DELETE
  @Path("{triggerId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("triggerId") String triggerId) {
    Trigger trigger = triggerService.get(appId, triggerId);
    if (trigger != null) {
      authorize(trigger, true);
      triggerService.delete(appId, triggerId);
    }
    return new RestResponse<>();
  }

  @GET
  @Path("{triggerId}/webhook_token")
  @Timed
  @ExceptionMetered
  public RestResponse<WebHookToken> generateWebhookToken(
      @QueryParam("appId") String appId, @PathParam("triggerId") String triggerId) {
    return new RestResponse<>(triggerService.generateWebHookToken(appId, triggerId));
  }

  @GET
  @Path("{triggerId}/webhook_token/git")
  @Timed
  @ExceptionMetered
  public RestResponse<WebHookToken> generateGitWebhookToken(
      @QueryParam("appId") String appId, @PathParam("triggerId") String triggerId) {
    return new RestResponse<>(triggerService.generateWebHookToken(appId, triggerId));
  }

  /**
   * Translate cron rest response.
   *
   * @param inputMap the input map
   * @return the rest response
   */
  @POST
  @Path("cron/translate")
  @Timed
  @ExceptionMetered
  public RestResponse<String> translateCron(Map<String, String> inputMap) {
    return new RestResponse<>(triggerService.getCronDescription(inputMap.get("expression")));
  }

  @GET
  @Path("webhook/parameters")
  @Timed
  @ExceptionMetered
  public RestResponse<WebhookParameters> listWebhookParameters(@QueryParam("appId") String appId,
      @QueryParam("workflowId") String workflowId, @QueryParam("workflowType") WorkflowType workflowType,
      @QueryParam("webhookSource") WebhookSource webhookSource, @QueryParam("eventType") WebhookEventType eventType) {
    return new RestResponse<>(
        triggerService.listWebhookParameters(appId, workflowId, workflowType, webhookSource, eventType));
  }

  @GET
  @Path("webhook/eventTypes")
  @Timed
  @ExceptionMetered
  public RestResponse<WebhookEventType> listWebhookEventTypes(@QueryParam("appId") String appId) {
    return new RestResponse<>();
  }
}
