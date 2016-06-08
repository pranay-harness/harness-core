package software.wings.resources;

import org.apache.commons.lang3.StringUtils;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionArgs.OrchestrationType;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.WorkflowService;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

// TODO: Auto-generated Javadoc

/**
 * The Class ExecutionResource.
 */
@Path("/executions")
public class ExecutionResource {
  private WorkflowService workflowService;

  /**
   * Instantiates a new execution resource.
   *
   * @param workflowService the workflow service
   */
  @Inject
  public ExecutionResource(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /**
   * List.
   *
   * @param appId           the app id
   * @param envId           the env id
   * @param orchestrationId the orchestration id
   * @param pageRequest     the page request
   * @return the rest response
   */
  @GET
  @Path("executions")
  @Produces("application/json")
  public RestResponse<PageResponse<WorkflowExecution>> listExecutions(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @QueryParam("orchestrationId") String orchestrationId,
      @BeanParam PageRequest<WorkflowExecution> pageRequest) {
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValues(appId);
    filter.setOp(Operator.EQ);
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
    return new RestResponse<>(workflowService.listExecutions(pageRequest, true));
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
  @Path("executions/{workflowExecutionId}")
  @Produces("application/json")
  public RestResponse<WorkflowExecution> getExecutionDetails(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("workflowExecutionId") String workflowExecutionId) {
    return new RestResponse<>(workflowService.getExecutionDetails(appId, workflowExecutionId));
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
  @Path("executions")
  @Produces("application/json")
  public RestResponse<WorkflowExecution> triggerExecution(
      @QueryParam("appId") String appId, @PathParam("envId") String envId, ExecutionArgs executionArgs) {
    return new RestResponse<>(workflowService.triggerEnvExecution(appId, envId, executionArgs));
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
  @Path("executions/orchestrated")
  @Produces("application/json")
  public RestResponse<WorkflowExecution> triggerOrchestratedExecution(
      @QueryParam("appId") String appId, @PathParam("envId") String envId, ExecutionArgs executionArgs) {
    executionArgs.setOrchestrationType(OrchestrationType.ORCHESTRATED);
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
  @Path("executions/simple")
  @Produces("application/json")
  public RestResponse<WorkflowExecution> triggerSimpleExecution(
      @QueryParam("appId") String appId, @PathParam("envId") String envId, ExecutionArgs executionArgs) {
    executionArgs.setOrchestrationType(OrchestrationType.SIMPLE);
    return triggerExecution(appId, envId, executionArgs);
  }

  /**
   * Update execution details rest response.
   *
   * @param appId               the app id
   * @param envId               the env id
   * @param workflowExecutionId the workflow execution id
   * @return the rest response
   */
  @PUT
  @Path("executions/{workflowExecutionId}")
  @Produces("application/json")
  public RestResponse<WorkflowExecution> updateExecutionDetails(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("workflowExecutionId") String workflowExecutionId) {
    // TODO - implement abort and pause functionality
    return null;
  }
}
