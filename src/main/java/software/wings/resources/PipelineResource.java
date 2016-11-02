/**
 *
 */

package software.wings.resources;

import io.swagger.annotations.Api;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
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
@PublicApi // TODO: remove it
public class PipelineResource {
  private WorkflowService workflowService;
  private WorkflowExecutionService workflowExecutionService;
  private PipelineService pipelineService;

  /**
   * Instantiates a new pipeline resource.
   *
   * @param workflowService          the workflow service
   * @param workflowExecutionService the workflow execution service
   * @param pipelineService          the pipeline service
   */
  @Inject
  public PipelineResource(WorkflowService workflowService, WorkflowExecutionService workflowExecutionService,
      PipelineService pipelineService) {
    this.workflowService = workflowService;
    this.workflowExecutionService = workflowExecutionService;
    this.pipelineService = pipelineService;
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
   * List executions rest response.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Path("executions")
  public RestResponse<PageResponse<PipelineExecution>> listExecutions(
      @BeanParam PageRequest<PipelineExecution> pageRequest) {
    return new RestResponse<>(pipelineService.listPipelineExecutions(pageRequest));
  }

  /**
   * Trigger execution rest response.
   *
   * @param appId      the app id
   * @param pipelineId the pipeline id
   * @return the rest response
   */
  @POST
  @Path("executions")
  public RestResponse<WorkflowExecution> triggerExecution(
      @QueryParam("appId") String appId, @QueryParam("pipelineId") String pipelineId) {
    return new RestResponse<>(pipelineService.execute(appId, pipelineId));
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
