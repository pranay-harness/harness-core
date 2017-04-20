/**
 *
 */

package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.RestResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.PipelineService;
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
  private PipelineService pipelineService;

  /**
   * Instantiates a new pipeline resource.
   *
   * @param workflowService the workflow service
   * @param pipelineService the pipeline service
   */
  @Inject
  public PipelineResource(WorkflowService workflowService, PipelineService pipelineService) {
    this.workflowService = workflowService;
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
  @Timed
  @ExceptionMetered
  @AuthRule(value = ResourceType.PIPELINE)
  public RestResponse<PageResponse<Pipeline>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Pipeline> pageRequest) {
    return new RestResponse<>(pipelineService.listPipelines(pageRequest));
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
  @Timed
  @ExceptionMetered
  @AuthRule(value = ResourceType.PIPELINE)
  public RestResponse<Pipeline> read(@QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId,
      @QueryParam("withServices") boolean withServices) {
    return new RestResponse<>(pipelineService.readPipeline(appId, pipelineId, withServices));
  }

  /**
   * Creates the.
   *
   * @param appId    the app id
   * @param pipeline the pipeline
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(value = ResourceType.PIPELINE)
  public RestResponse<Pipeline> create(@QueryParam("appId") String appId, Pipeline pipeline) {
    pipeline.setAppId(appId);
    return new RestResponse<>(pipelineService.createPipeline(pipeline));
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
  @Timed
  @ExceptionMetered
  @AuthRule(value = ResourceType.PIPELINE)
  public RestResponse<Pipeline> update(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId, Pipeline pipeline) {
    pipeline.setAppId(appId);
    pipeline.setUuid(pipelineId);
    return new RestResponse<>(pipelineService.updatePipeline(pipeline));
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
  @Timed
  @ExceptionMetered
  @AuthRule(value = ResourceType.PIPELINE)
  public RestResponse delete(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId, Pipeline pipeline) {
    pipelineService.deletePipeline(appId, pipelineId);
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
  @Timed
  @ExceptionMetered
  @AuthRule(value = ResourceType.CD)
  public RestResponse<PageResponse<PipelineExecution>> listExecutions(
      @BeanParam PageRequest<PipelineExecution> pageRequest) {
    return new RestResponse<>(pipelineService.listPipelineExecutions(pageRequest));
  }

  /**
   * Trigger execution rest response.
   *
   * @param appId         the app id
   * @param pipelineId    the pipeline id
   * @param executionArgs the execution args
   * @return the rest response
   */
  @POST
  @Path("executions")
  @Timed
  @ExceptionMetered
  @AuthRule(value = ResourceType.CD)
  public RestResponse<WorkflowExecution> triggerExecution(
      @QueryParam("appId") String appId, @QueryParam("pipelineId") String pipelineId, ExecutionArgs executionArgs) {
    return new RestResponse<>(pipelineService.execute(appId, pipelineId, executionArgs));
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
  @Timed
  @ExceptionMetered
  @AuthRule(value = ResourceType.PIPELINE)
  public RestResponse<List<Stencil>> stencils(@QueryParam("appId") String appId, @QueryParam("envId") String envId) {
    return new RestResponse<>(workflowService.stencils(appId, null, null, StateTypeScope.PIPELINE_STENCILS)
                                  .get(StateTypeScope.PIPELINE_STENCILS));
  }
}
