/**
 *
 */

package software.wings.resources;

import static io.harness.eraro.ErrorCode.DUPLICATE_STATE_NAMES;
import static io.harness.exception.WingsException.ReportTarget.LOG_SYSTEM;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.exception.WingsException;
import io.swagger.annotations.Api;
import software.wings.beans.EntityType;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateTypeScope;
import software.wings.stencils.Stencil;

import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
@Scope(APPLICATION)
@AuthRule(permissionType = PermissionType.PIPELINE)
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
   * @param appIds       the app ids
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Pipeline>> list(@QueryParam("appId") List<String> appIds,
      @BeanParam PageRequest<Pipeline> pageRequest,
      @QueryParam("previousExecutionsCount") Integer previousExecutionsCount,
      @QueryParam("details") @DefaultValue("true") boolean details) {
    return new RestResponse<>(pipelineService.listPipelines(pageRequest, details, previousExecutionsCount));
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
  public RestResponse<Pipeline> create(@QueryParam("appId") String appId, Pipeline pipeline) {
    pipeline.setAppId(appId);
    return new RestResponse<>(pipelineService.save(pipeline));
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
  public RestResponse<Pipeline> update(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId, Pipeline pipeline) {
    pipeline.setAppId(appId);
    pipeline.setUuid(pipelineId);
    try {
      return new RestResponse<>(pipelineService.update(pipeline));
    } catch (WingsException exception) {
      // When the pipeline update is coming from the user there is no harness engineer wrong doing to alerted for
      exception.excludeReportTarget(DUPLICATE_STATE_NAMES, LOG_SYSTEM);
      throw exception;
    }
  }

  /**
   * Update.
   *
   * @param appId             the app id
   * @param pipelineId        the pipeline id
   * @param failureStrategies the failureStrategies
   * @return the rest response
   */
  @PUT
  @Path("{pipelineId}/failure-strategies")
  @Timed
  @ExceptionMetered
  public RestResponse<List<FailureStrategy>> updateFailureStrategies(@QueryParam("appId") String appId,
      @PathParam("pipelineId") String pipelineId, List<FailureStrategy> failureStrategies) {
    return new RestResponse<>(pipelineService.updateFailureStrategies(appId, pipelineId, failureStrategies));
  }

  @POST
  @Path("{pipelineId}/clone")
  @Timed
  @ExceptionMetered
  public RestResponse<Pipeline> read(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId, Pipeline pipeline) {
    pipeline.setAppId(appId);
    return new RestResponse<>(pipelineService.clonePipeline(pipelineId, pipeline));
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
  public RestResponse delete(
      @QueryParam("appId") String appId, @PathParam("pipelineId") String pipelineId, Pipeline pipeline) {
    pipelineService.deletePipeline(appId, pipelineId);
    return new RestResponse();
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
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<List<Stencil>> stencils(@QueryParam("appId") String appId, @QueryParam("envId") String envId) {
    return new RestResponse<>(workflowService.stencils(appId, null, null, StateTypeScope.PIPELINE_STENCILS)
                                  .get(StateTypeScope.PIPELINE_STENCILS));
  }

  /**
   * Required args rest response.
   *
   * @param appId         the app id
   * @param pipelineId    the pipelineId
   * @return the rest response
   */
  @GET
  @Path("required-entities")
  @Timed
  @ExceptionMetered
  public RestResponse<List<EntityType>> requiredEntities(
      @QueryParam("appId") String appId, @QueryParam("pipelineId") String pipelineId) {
    return new RestResponse<>(pipelineService.getRequiredEntities(appId, pipelineId));
  }
}
