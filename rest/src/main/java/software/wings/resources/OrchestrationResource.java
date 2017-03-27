/**
 *
 */

package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Orchestration;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
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
 * The Class OrchestrationResource.
 *
 * @author Rishi
 */
@Api("orchestrations")
@Path("/orchestrations")
@Produces("application/json")
public class OrchestrationResource {
  private WorkflowService workflowService;

  /**
   * Instantiates a new orchestration resource.
   *
   * @param workflowService the workflow service
   */
  @Inject
  public OrchestrationResource(WorkflowService workflowService) {
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
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Orchestration>> list(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @BeanParam PageRequest<Orchestration> pageRequest) {
    pageRequest.addFilter("appId", appId, SearchFilter.Operator.EQ);
    return new RestResponse<>(workflowService.listOrchestration(pageRequest, envId));
  }

  /**
   * Read.
   *
   * @param appId           the app id
   * @param orchestrationId the orchestration id
   * @return the rest response
   */
  @GET
  @Path("{orchestrationId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Orchestration> read(@QueryParam("appId") String appId,
      @PathParam("orchestrationId") String orchestrationId, @QueryParam("version") Integer version) {
    return new RestResponse<>(workflowService.readOrchestration(appId, orchestrationId, version));
  }

  /**
   * Creates the.
   *
   * @param appId         the app id
   * @param envId         the env id
   * @param orchestration the orchestration
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Orchestration> create(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, Orchestration orchestration) {
    orchestration.setAppId(appId);
    orchestration.setWorkflowType(WorkflowType.ORCHESTRATION);
    return new RestResponse<>(workflowService.createWorkflow(Orchestration.class, orchestration));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param orchestrationId the orchestration id
   * @param orchestration   the orchestration
   * @return the rest response
   */
  @PUT
  @Path("{orchestrationId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Orchestration> update(@QueryParam("appId") String appId,
      @PathParam("orchestrationId") String orchestrationId, Orchestration orchestration) {
    orchestration.setAppId(appId);
    orchestration.setUuid(orchestrationId);
    return new RestResponse<>(workflowService.updateOrchestration(orchestration));
  }

  /**
   * Delete.
   *
   * @param appId           the app id
   * @param orchestrationId the orchestration id
   * @return the rest response
   */
  @DELETE
  @Path("{orchestrationId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("orchestrationId") String orchestrationId) {
    workflowService.deleteWorkflow(Orchestration.class, appId, orchestrationId);
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
  public RestResponse<List<Stencil>> stencils(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @QueryParam("workflowId") String workflowId, @QueryParam("phaseId") String phaseId) {
    return new RestResponse<>(
        workflowService.stencils(appId, workflowId, phaseId, StateTypeScope.ORCHESTRATION_STENCILS)
            .get(StateTypeScope.ORCHESTRATION_STENCILS));
  }
}
