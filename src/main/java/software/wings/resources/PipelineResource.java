/**
 *
 */
package software.wings.resources;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionType;
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

/**
 * @author Rishi
 */
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
    return new RestResponse<PageResponse<Pipeline>>(workflowService.listPipelines(pageRequest));
  }

  @GET
  @Path("{pipelineId}")
  @Produces("application/json")
  public RestResponse<Pipeline> read(@QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId) {
    return new RestResponse<Pipeline>(workflowService.readPipeline(appId, pipelineId));
  }

  @POST
  @Produces("application/json")
  public RestResponse<Pipeline> create(@QueryParam("appId") String appId, Pipeline pipeline) {
    pipeline.setAppId(appId);
    return new RestResponse<Pipeline>(workflowService.createWorkflow(Pipeline.class, pipeline));
  }

  @PUT
  @Path("{pipelineId}")
  @Produces("application/json")
  public RestResponse<Pipeline> update(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId, Pipeline pipeline) {
    pipeline.setAppId(appId);
    return new RestResponse<Pipeline>(workflowService.updateWorkflow(Pipeline.class, pipeline));
  }

  @GET
  @Path("executions")
  @Produces("application/json")
  public RestResponse<PageResponse<WorkflowExecution>> listExecutions(@QueryParam("appId") String appId,
      @QueryParam("pipelineId") String pipelineId, @BeanParam PageRequest<WorkflowExecution> pageRequest) {
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValue(appId);
    filter.setOp(Operator.EQ);
    pageRequest.getFilters().add(filter);

    filter = new SearchFilter();
    filter.setFieldName("workflowExecutionType");
    filter.setFieldValue(WorkflowExecutionType.PIPELINE);
    filter.setOp(Operator.EQ);
    pageRequest.getFilters().add(filter);

    return new RestResponse<PageResponse<WorkflowExecution>>(workflowService.listExecutions(pageRequest, true));
  }

  @POST
  @Path("executions")
  @Produces("application/json")
  public RestResponse<WorkflowExecution> triggerExecution(
      @QueryParam("appId") String appId, @QueryParam("pipelineId") String pipelineId) {
    return new RestResponse<WorkflowExecution>(workflowService.triggerPipelineExecution(appId, pipelineId));
  }
}
