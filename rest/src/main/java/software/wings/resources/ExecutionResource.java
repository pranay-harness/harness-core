package software.wings.resources;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Application;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Graph.Node;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionInterrupt;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
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
@AuthRule(ResourceType.APPLICATION)
public class ExecutionResource {
  private AppService appService;
  private WorkflowExecutionService workflowExecutionService;

  /**
   * Instantiates a new execution resource.
   *
   * @param appService               the app service
   * @param workflowExecutionService the workflow service
   */
  @Inject
  public ExecutionResource(AppService appService, WorkflowExecutionService workflowExecutionService) {
    this.appService = appService;
    this.workflowExecutionService = workflowExecutionService;
  }

  /**
   * List.
   *
   * @param appId           the app id
   * @param envId           the env id
   * @param orchestrationId the orchestration id
   * @param pageRequest     the page request
   * @param includeGraph    the include graph\
   * @return the rest response
   */
  @GET
  @Produces("application/json")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<WorkflowExecution>> listExecutions(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @QueryParam("orchestrationId") String orchestrationId, @BeanParam PageRequest<WorkflowExecution> pageRequest,
      @DefaultValue("true") @QueryParam("includeGraph") boolean includeGraph) {
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    if (isBlank(appId)) {
      PageRequest<Application> applicationPageRequest =
          aPageRequest().addFieldsIncluded("uuid").addFilter("accountId", Operator.EQ, accountId).build();
      PageResponse<Application> res = appService.list(applicationPageRequest, false, 0, 0);
      if (res == null || res.isEmpty()) {
        return new RestResponse<PageResponse<WorkflowExecution>>(new PageResponse<WorkflowExecution>());
      }
      List<String> appIds = res.stream().map(Application::getUuid).collect(Collectors.toList());
      filter.setFieldValues(appIds.toArray());
    } else {
      filter.setFieldValues(appId);
    }
    filter.setOp(Operator.IN);
    pageRequest.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("workflowType");
    filter.setFieldValues(WorkflowType.ORCHESTRATION, WorkflowType.SIMPLE);
    filter.setOp(Operator.IN);
    pageRequest.addFilter(filter);

    if (StringUtils.isNotBlank(orchestrationId)) {
      filter = new SearchFilter();
      filter.setFieldName("workflowId");
      filter.setFieldValues(orchestrationId);
      filter.setOp(Operator.EQ);
      pageRequest.addFilter(filter);
    }
    return new RestResponse<>(workflowExecutionService.listExecutions(pageRequest, includeGraph, true, true));
  }

  /**
   * Gets execution details.
   *
   * @param appId               the app id
   * @param envId               the env id
   * @param workflowExecutionId the workflow execution id
   * @param expandedGroupIds    the expanded group ids
   * @return the execution details
   */
  @GET
  @Path("{workflowExecutionId}")
  @Produces("application/json")
  @Timed
  @ExceptionMetered
  public RestResponse<WorkflowExecution> getExecutionDetails(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("workflowExecutionId") String workflowExecutionId,
      @QueryParam("expandedGroupId") List<String> expandedGroupIds) {
    return new RestResponse<>(
        workflowExecutionService.getExecutionDetails(appId, workflowExecutionId, expandedGroupIds));
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
  @Produces("application/json")
  @Timed
  @ExceptionMetered
  public RestResponse<WorkflowExecution> triggerExecution(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, ExecutionArgs executionArgs) {
    return new RestResponse<>(workflowExecutionService.triggerEnvExecution(appId, envId, executionArgs));
  }

  /**
   * Trigger orchestrated execution rest response.
   *
   * @param appId         the app id
   * @param envId         the env id
   * @param executionArgs the execution args
   * @return the rest response
   */
  @POST
  @Path("orchestrated")
  @Produces("application/json")
  @Timed
  @ExceptionMetered
  public RestResponse<WorkflowExecution> triggerOrchestratedExecution(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, ExecutionArgs executionArgs) {
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    return triggerExecution(appId, envId, executionArgs);
  }

  /**
   * Trigger simple execution rest response.
   *
   * @param appId         the app id
   * @param envId         the env id
   * @param executionArgs the execution args
   * @return the rest response
   */
  @POST
  @Path("simple")
  @Produces("application/json")
  @Timed
  @ExceptionMetered
  public RestResponse<WorkflowExecution> triggerSimpleExecution(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, ExecutionArgs executionArgs) {
    executionArgs.setWorkflowType(WorkflowType.SIMPLE);
    return triggerExecution(appId, envId, executionArgs);
  }

  /**
   * Update execution details rest response.
   *
   * @param appId               the app id
   * @param envId               the env id
   * @param workflowExecutionId the workflow execution id
   * @param executionInterrupt      the execution event
   * @return the rest response
   */
  @PUT
  @Path("{workflowExecutionId}")
  @Produces("application/json")
  @Timed
  @ExceptionMetered
  public RestResponse<ExecutionInterrupt> triggerWorkflowExecutionInterrupt(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("workflowExecutionId") String workflowExecutionId,
      ExecutionInterrupt executionInterrupt) {
    executionInterrupt.setAppId(appId);
    executionInterrupt.setEnvId(envId);
    executionInterrupt.setExecutionUuid(workflowExecutionId);

    return new RestResponse<>(workflowExecutionService.triggerExecutionInterrupt(executionInterrupt));
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
  @Produces("application/json")
  @Timed
  @ExceptionMetered
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
  @Produces("application/json")
  @Timed
  @ExceptionMetered
  public RestResponse<Node> getExecutionNodeDetails(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("workflowExecutionId") String workflowExecutionId,
      @PathParam("stateExecutionInstanceId") String stateExecutionInstanceId) {
    return new RestResponse<>(
        workflowExecutionService.getExecutionDetailsForNode(appId, workflowExecutionId, stateExecutionInstanceId));
  }
}
