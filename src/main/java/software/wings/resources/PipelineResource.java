/**
 *
 */
package software.wings.resources;

import io.swagger.annotations.Api;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.WorkflowService;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * @author Rishi
 */
@Api("pipelines")
@Path("/pipelines")
public class PipelineResource {
  private WorkflowService workflowService;

  @Inject
  public PipelineResource(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @GET
  @Produces("application/json")
  public RestResponse<PageResponse<Pipeline>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Pipeline> pageRequest) {
    pageRequest.addFilter("appId", appId, SearchFilter.Operator.EQ);
    return new RestResponse<>(workflowService.listPipelines(pageRequest));
  }

  @GET
  @Path("{pipelineId}")
  @Produces("application/json")
  public RestResponse<Pipeline> read(@QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId) {
    return new RestResponse<>(workflowService.readPipeline(appId, pipelineId));
  }

  @POST
  @Produces("application/json")
  public RestResponse<Pipeline> create(@QueryParam("appId") String appId, Pipeline pipeline) {
    pipeline.setAppId(appId);
    return new RestResponse<>(workflowService.createWorkflow(Pipeline.class, pipeline));
  }

  @PUT
  @Path("{pipelineId}")
  @Produces("application/json")
  public RestResponse<Pipeline> update(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId, Pipeline pipeline) {
    pipeline.setAppId(appId);
    pipeline.setUuid(pipelineId);
    return new RestResponse<>(workflowService.updatePipeline(pipeline));
  }

  @DELETE
  @Path("{pipelineId}")
  @Produces("application/json")
  public RestResponse delete(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId, Pipeline pipeline) {
    workflowService.deleteWorkflow(Pipeline.class, appId, pipelineId);
    return new RestResponse();
  }

  @GET
  @Path("{pipelineId}/executions")
  @Produces("application/json")
  public RestResponse<PageResponse<WorkflowExecution>> listExecutions(@QueryParam("appId") String appId,
      @PathParam("pipelineId") String pipelineId, @BeanParam PageRequest<WorkflowExecution> pageRequest) {
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValues(appId);
    filter.setOp(Operator.EQ);
    pageRequest.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("workflowExecutionType");
    filter.setFieldValues(WorkflowExecutionType.PIPELINE);
    filter.setOp(Operator.EQ);
    pageRequest.addFilter(filter);

    return new RestResponse<>(workflowService.listExecutions(pageRequest, true));
  }

  @POST
  @Path("{pipelineId}/executions")
  @Produces("application/json")
  public RestResponse<WorkflowExecution> triggerExecution(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId) {
    return new RestResponse<>(workflowService.triggerPipelineExecution(appId, pipelineId));
  }
}
