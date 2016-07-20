/**
 *
 */

package software.wings.resources;

import io.swagger.annotations.Api;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateTypeScope;
import software.wings.stencils.Stencil;

import java.util.List;
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
 * The Class PipelineResource.
 *
 * @author Rishi
 */
@Api("pipelines")
@Path("/pipelines")
@Produces("application/json")
public class PipelineResource {
  private WorkflowService workflowService;

  /**
   * Instantiates a new pipeline resource.
   *
   * @param workflowService the workflow service
   */
  @Inject
  public PipelineResource(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /**
   * List.
   *
   * @param appId       the app id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<Pipeline>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Pipeline> pageRequest) {
    pageRequest.addFilter("appId", appId, SearchFilter.Operator.EQ);
    return new RestResponse<>(workflowService.listPipelines(pageRequest));
  }

  /**
   * Read.
   *
   * @param appId      the app id
   * @param pipelineId the pipeline id
   * @return the rest response
   */
  @GET
  @Path("{pipelineId}")
  public RestResponse<Pipeline> read(@QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId) {
    return new RestResponse<>(workflowService.readPipeline(appId, pipelineId));
  }

  /**
   * Creates the.
   *
   * @param appId    the app id
   * @param pipeline the pipeline
   * @return the rest response
   */
  @POST
  public RestResponse<Pipeline> create(@QueryParam("appId") String appId, Pipeline pipeline) {
    pipeline.setAppId(appId);
    return new RestResponse<>(workflowService.createWorkflow(Pipeline.class, pipeline));
  }

  /**
   * Update.
   *
   * @param appId      the app id
   * @param pipelineId the pipeline id
   * @param pipeline   the pipeline
   * @return the rest response
   */
  @PUT
  @Path("{pipelineId}")
  public RestResponse<Pipeline> update(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId, Pipeline pipeline) {
    pipeline.setAppId(appId);
    pipeline.setUuid(pipelineId);
    return new RestResponse<>(workflowService.updatePipeline(pipeline));
  }

  /**
   * Delete.
   *
   * @param appId      the app id
   * @param pipelineId the pipeline id
   * @param pipeline   the pipeline
   * @return the rest response
   */
  @DELETE
  @Path("{pipelineId}")
  public RestResponse delete(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId, Pipeline pipeline) {
    workflowService.deleteWorkflow(Pipeline.class, appId, pipelineId);
    return new RestResponse();
  }

  /**
   * List executions.
   *
   * @param appId       the app id
   * @param pipelineId  the pipeline id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Path("{pipelineId}/executions")
  public RestResponse<PageResponse<WorkflowExecution>> listExecutions(@QueryParam("appId") String appId,
      @PathParam("pipelineId") String pipelineId, @BeanParam PageRequest<WorkflowExecution> pageRequest) {
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValues(appId);
    filter.setOp(Operator.EQ);
    pageRequest.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("workflowType");
    filter.setFieldValues(WorkflowType.PIPELINE);
    filter.setOp(Operator.EQ);
    pageRequest.addFilter(filter);

    return new RestResponse<>(workflowService.listExecutions(pageRequest, true));
  }

  /**
   * Trigger execution.
   *
   * @param appId         the app id
   * @param pipelineId    the pipeline id
   * @param executionArgs the execution args
   * @return the rest response
   */
  @POST
  @Path("{pipelineId}/executions")
  public RestResponse<WorkflowExecution> triggerExecution(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId, ExecutionArgs executionArgs) {
    return new RestResponse<>(workflowService.triggerPipelineExecution(appId, pipelineId));
  }

  /**
   * Stencils rest response.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the rest response
   */
  @GET
  @Path("stencils")
  public RestResponse<List<Stencil>> stencils(@QueryParam("appId") String appId, @QueryParam("envId") String envId) {
    return new RestResponse<>(
        workflowService.stencils(appId, StateTypeScope.PIPELINE_STENCILS).get(StateTypeScope.PIPELINE_STENCILS));
  }
}
