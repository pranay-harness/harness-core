package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.security.PermissionAttribute.Action.EXECUTE;
import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import io.swagger.annotations.Api;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.ApprovalAuthorization;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.RestResponse;
import software.wings.beans.StateExecutionElement;
import software.wings.beans.StateExecutionInterrupt;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.common.Constants;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.StateExecutionData;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Set;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
/**
 * The Class ExecutionResource.
 */
@Api("executions")
@Path("/executions")
@Scope(ResourceType.APPLICATION)
@Produces("application/json")
public class ExecutionResource {
  private AppService appService;
  private WorkflowExecutionService workflowExecutionService;
  private AuthHandler authHandler;

  /**
   * Instantiates a new execution resource.
   *
   * @param appService               the app service
   * @param workflowExecutionService the workflow service
   */
  @Inject
  public ExecutionResource(
      AppService appService, WorkflowExecutionService workflowExecutionService, AuthHandler authHandler) {
    this.appService = appService;
    this.workflowExecutionService = workflowExecutionService;
    this.authHandler = authHandler;
  }

  /**
   * List.
   *
   * @param appIds           the app ids
   * @param envId           the env id
   * @param orchestrationId the orchestration id
   * @param pageRequest     the page request
   * @param includeGraph    the include graph\
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = READ, parameterName = "orchestrationId", dbFieldName = "workflowId",
      dbCollectionName = "software.wings.beans.WorkflowExecution", skipAuth = true)
  public RestResponse<PageResponse<WorkflowExecution>>
  listExecutions(@QueryParam("accountId") String accountId, @QueryParam("appId") List<String> appIds,
      @QueryParam("envId") String envId, @QueryParam("orchestrationId") String orchestrationId,
      @BeanParam PageRequest<WorkflowExecution> pageRequest,
      @DefaultValue("false") @QueryParam("includeGraph") boolean includeGraph,
      @QueryParam("workflowType") List<String> workflowTypes,
      @DefaultValue("false") @QueryParam("includeIndirectExecutions") boolean includeIndirectExecutions) {
    List<String> authorizedAppIds = null;
    if (isNotEmpty(appIds)) {
      authorizedAppIds = appIds;
    } else {
      authorizedAppIds = appService.getAppIdsByAccountId(accountId);
    }

    pageRequest.addFilter("appId", Operator.IN, authorizedAppIds.toArray());

    if (pageRequest.getPageSize() > Constants.DEFAULT_RUNTIME_ENTITY_PAGESIZE) {
      pageRequest.setLimit(Constants.DEFAULT_RUNTIME_ENTITY_PAGESIZE_STR);
    }

    if (isNotEmpty(workflowTypes)) {
      pageRequest.addFilter("workflowType", Operator.IN, workflowTypes.toArray());
    }

    // No need to show child executions unless includeIndirectExecutions is true
    if (!includeIndirectExecutions) {
      pageRequest.addFilter("pipelineExecutionId", Operator.NOT_EXISTS);
    }

    if (isNotBlank(orchestrationId)) {
      pageRequest.addFilter("workflowId", Operator.EQ, orchestrationId);
    }
    return new RestResponse<>(workflowExecutionService.listExecutions(pageRequest, includeGraph, true, true, false));
  }

  /**
   * Gets execution details.
   *
   * @param appId               the app id
   * @param envId               the env id
   * @param workflowExecutionId the workflow execution id
   * @return the execution details
   */
  @GET
  @Path("{workflowExecutionId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = READ, skipAuth = true)
  public RestResponse<WorkflowExecution> getExecutionDetails(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("workflowExecutionId") String workflowExecutionId,
      @QueryParam("excludeFromAggregation") Set<String> excludeFromAggregation) {
    return new RestResponse<>(
        workflowExecutionService.getExecutionDetails(appId, workflowExecutionId, false, excludeFromAggregation));
  }

  /**
   * Save.
   *
   * @param appId         the app id
   * @param envId         the env id
   * @param executionArgs the execution args
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  // We are handling the check programmatically for now, since we don't have enough info in the query / path parameters
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE, skipAuth = true)
  public RestResponse<WorkflowExecution> triggerExecution(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @QueryParam("pipelineId") String pipelineId, ExecutionArgs executionArgs) {
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.DEPLOYMENT, Action.EXECUTE);
    List<PermissionAttribute> permissionAttributeList = asList(permissionAttribute);
    if (pipelineId != null && executionArgs.getWorkflowType() == WorkflowType.PIPELINE) {
      executionArgs.setPipelineId(pipelineId);
      authHandler.authorize(permissionAttributeList, asList(appId), pipelineId);
    } else {
      if (executionArgs != null) {
        if (executionArgs.getOrchestrationId() != null) {
          authHandler.authorize(permissionAttributeList, asList(appId), executionArgs.getOrchestrationId());
        } else if (executionArgs.getPipelineId() != null) {
          authHandler.authorize(permissionAttributeList, asList(appId), executionArgs.getPipelineId());
        }
      }
    }

    return new RestResponse<>(workflowExecutionService.triggerEnvExecution(appId, envId, executionArgs, null));
  }

  /**
   * Update execution details rest response.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   * @param executionInterrupt      the execution event
   * @return the rest response
   */
  @PUT
  @Path("{workflowExecutionId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE, skipAuth = true)
  public RestResponse<ExecutionInterrupt> triggerWorkflowExecutionInterrupt(@QueryParam("appId") String appId,
      @PathParam("workflowExecutionId") String workflowExecutionId, ExecutionInterrupt executionInterrupt) {
    executionInterrupt.setAppId(appId);
    executionInterrupt.setExecutionUuid(workflowExecutionId);
    authorize(appId, workflowExecutionId, EXECUTE);
    return new RestResponse<>(workflowExecutionService.triggerExecutionInterrupt(executionInterrupt));
  }

  private void authorize(String appId, String workflowExecutionId, Action requiredAction) {
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.DEPLOYMENT, requiredAction);
    List<PermissionAttribute> permissionAttributeList = asList(permissionAttribute);
    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetailsWithoutGraph(appId, workflowExecutionId);
    if (workflowExecution.getPipelineSummary() != null) {
      String pipelineId = workflowExecution.getPipelineSummary().getPipelineId();
      Validator.notNullCheck("Pipeline id is null for execution " + workflowExecutionId, pipelineId);
      authHandler.authorize(permissionAttributeList, asList(appId), pipelineId);
    } else {
      String workflowId = workflowExecution.getWorkflowId();
      Validator.notNullCheck("Workflow id is null for execution " + workflowExecutionId, workflowId);
      authHandler.authorize(permissionAttributeList, asList(appId), workflowId);
    }
  }

  /**
   * Trigger execution rest response.
   *
   * @param appId         the app id
   * @param workflowExecutionId    the workflowExecutionId
   * @param executionArgs the Execution Args
   * @return the rest response
   */
  @PUT
  @Path("{workflowExecutionId}/notes")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE, skipAuth = true)
  public RestResponse<Boolean> updateNotes(@QueryParam("appId") String appId,
      @PathParam("workflowExecutionId") String workflowExecutionId, ExecutionArgs executionArgs) {
    authorize(appId, workflowExecutionId, EXECUTE);
    return new RestResponse<>(workflowExecutionService.updateNotes(appId, workflowExecutionId, executionArgs));
  }

  /**
   * Trigger execution rest response.
   *
   * @param appId         the app id
   * @param workflowExecutionId    the workflowExecutionId
   * @param approvalDetails the Approval User details
   * @return the rest response
   */
  @PUT
  @Path("{workflowExecutionId}/approval")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE, skipAuth = true)
  public RestResponse approveOrRejectExecution(@QueryParam("appId") String appId,
      @QueryParam("stateExecutionId") String stateExecutionId,
      @PathParam("workflowExecutionId") String workflowExecutionId, ApprovalDetails approvalDetails) {
    ApprovalStateExecutionData approvalStateExecutionData =
        workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
            appId, workflowExecutionId, stateExecutionId, approvalDetails);

    if (isEmpty(approvalStateExecutionData.getUserGroups())) {
      authorize(appId, workflowExecutionId, EXECUTE);
    }

    return new RestResponse<>(workflowExecutionService.approveOrRejectExecution(
        appId, approvalStateExecutionData.getUserGroups(), approvalDetails));
  }

  /**
   * Required args rest response.
   *
   * @param appId         the app id
   * @param envId         the env id
   * @param executionArgs the execution args
   * @return the rest response
   */
  @POST
  @Path("required-args")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = READ, skipAuth = true)
  public RestResponse<RequiredExecutionArgs> requiredArgs(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, ExecutionArgs executionArgs) {
    return new RestResponse<>(workflowExecutionService.getRequiredExecutionArgs(appId, envId, executionArgs));
  }

  /**
   * Gets execution node details.
   *
   * @param appId                    the app id
   * @param envId                    the env id
   * @param workflowExecutionId      the workflow execution id
   * @param stateExecutionInstanceId the state execution instance id
   * @return the execution node details
   */
  @GET
  @Path("{workflowExecutionId}/node/{stateExecutionInstanceId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = READ, skipAuth = true)
  public RestResponse<GraphNode> getExecutionNodeDetails(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("workflowExecutionId") String workflowExecutionId,
      @PathParam("stateExecutionInstanceId") String stateExecutionInstanceId) {
    return new RestResponse<>(
        workflowExecutionService.getExecutionDetailsForNode(appId, workflowExecutionId, stateExecutionInstanceId));
  }

  /**
   * Gets execution history list.
   *
   * @param appId                    the app id
   * @param workflowExecutionId      the workflow execution id
   * @param stateExecutionInstanceId the state execution instance id
   * @return the execution history list
   */
  @GET
  @Path("{workflowExecutionId}/history/{stateExecutionInstanceId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = READ, skipAuth = true)
  public RestResponse<List<StateExecutionData>> getExecutionHistory(@QueryParam("appId") String appId,
      @PathParam("workflowExecutionId") String workflowExecutionId,
      @PathParam("stateExecutionInstanceId") String stateExecutionInstanceId) {
    return new RestResponse<>(
        workflowExecutionService.getExecutionHistory(appId, workflowExecutionId, stateExecutionInstanceId));
  }

  /**
   * Gets execution history list.
   *
   * @param appId                    the app id
   * @param workflowExecutionId      the workflow execution id
   * @param stateExecutionInstanceId the state execution instance id
   * @return the execution history list
   */
  @GET
  @Path("{workflowExecutionId}/interruption/{stateExecutionInstanceId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = READ, skipAuth = true)
  public RestResponse<List<StateExecutionInterrupt>> getExecutionInterrupt(@QueryParam("appId") String appId,
      @PathParam("workflowExecutionId") String workflowExecutionId,
      @PathParam("stateExecutionInstanceId") String stateExecutionInstanceId) {
    return new RestResponse<>(workflowExecutionService.getExecutionInterrupts(appId, stateExecutionInstanceId));
  }

  /**
   * Gets execution history list.
   *
   * @param appId                    the app id
   * @param workflowExecutionId      the workflow execution id
   * @param stateExecutionInstanceId the state execution instance id
   * @return the execution history list
   */
  @GET
  @Path("{workflowExecutionId}/element/{stateExecutionInstanceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<List<StateExecutionElement>> getExecutionElement(@QueryParam("appId") String appId,
      @PathParam("workflowExecutionId") String workflowExecutionId,
      @PathParam("stateExecutionInstanceId") String stateExecutionInstanceId) {
    return new RestResponse<>(workflowExecutionService.getExecutionElements(appId, stateExecutionInstanceId));
  }

  /**
   * Marks the pipeline as baseline for verification steps
   * @param appId
   * @param workflowExecutionId
   * @return
   */
  @GET
  @Path("{workflowExecutionId}/mark-baseline")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE, skipAuth = true)
  public RestResponse<Set<WorkflowExecutionBaseline>> markAsBaseline(@QueryParam("appId") String appId,
      @QueryParam("isBaseline") boolean isBaseline, @PathParam("workflowExecutionId") String workflowExecutionId) {
    authorize(appId, workflowExecutionId, EXECUTE);
    return new RestResponse<>(workflowExecutionService.markBaseline(appId, workflowExecutionId, isBaseline));
  }

  /**
   * gets the details for baseline execution
   * @param appId
   * @param baselineExecutionId
   * @return
   */
  @GET
  @Path("{workflowExecutionId}/get-baseline")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = READ, skipAuth = true)
  public RestResponse<WorkflowExecutionBaseline> getBaselineDetails(@QueryParam("appId") String appId,
      @PathParam("workflowExecutionId") String baselineExecutionId,
      @QueryParam("stateExecutionId") String stateExecutionId, @QueryParam("currentExecId") String currentExecId) {
    return new RestResponse<>(
        workflowExecutionService.getBaselineDetails(appId, baselineExecutionId, stateExecutionId, currentExecId));
  }

  @GET
  @Path("artifacts")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = READ, skipAuth = true)
  public RestResponse<List<Artifact>> getLastDeployedArtifacts(
      @QueryParam("appId") String appId, @QueryParam("workflowId") String workflowId) {
    return new RestResponse<>(workflowExecutionService.obtainLastGoodDeployedArtifacts(appId, workflowId));
  }

  /**
   * Returns the list of either running or queued executions on the requested
   * execution
   * @param appId
   * @param workflowExecutionId
   * @return
   */
  @GET
  @Path("{workflowExecutionId}/waitingOnDeployments")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = READ, skipAuth = true)
  public RestResponse<List<WorkflowExecution>> getWaitingOnDeployments(
      @QueryParam("appId") String appId, @PathParam("workflowExecutionId") String workflowExecutionId) {
    return new RestResponse<>(workflowExecutionService.listWaitingOnDeployments(appId, workflowExecutionId));
  }

  @GET
  @Path("{workflowExecutionId}/approvalAuthorization")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE, skipAuth = true)
  public RestResponse<ApprovalAuthorization> getApprovalAuthorization(@QueryParam("appId") String appId,
      @PathParam("workflowExecutionId") String workflowExecutionId, @QueryParam("userGroups") List<String> userGroups) {
    // In case of approvals with no user groups, we want to make sure that the user has permission to access that
    // workflow/pipeline. Below empty check does that
    notNullCheck("App cannot be null", appId);

    if (isEmpty(userGroups)) {
      try {
        authorize(appId, workflowExecutionId, EXECUTE);
      } catch (WingsException e) {
        ApprovalAuthorization approvalAuthorization = new ApprovalAuthorization();
        approvalAuthorization.setAuthorized(false);
        return new RestResponse<>(approvalAuthorization);
      }
    }

    return new RestResponse<>(workflowExecutionService.getApprovalAuthorization(appId, userGroups));
  }
}
