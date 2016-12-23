/**
 *
 */

package software.wings.resources;

import io.swagger.annotations.Api;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.RestResponse;
import software.wings.beans.WorkflowOuterSteps;
import software.wings.beans.WorkflowPhase;
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
 * The Class OrchestrationResource.
 *
 * @author Rishi
 */
@Api("workflows")
@Path("/workflows")
@Produces("application/json")
public class WorkflowResource {
  private WorkflowService workflowService;

  /**
   * Instantiates a new orchestration resource.
   *
   * @param workflowService the workflow service
   */
  @Inject
  public WorkflowResource(WorkflowService workflowService) {
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
  public RestResponse<PageResponse<OrchestrationWorkflow>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<OrchestrationWorkflow> pageRequest) {
    return new RestResponse<>(workflowService.listOrchestrationWorkflows(pageRequest));
  }

  /**
   * Read.
   *
   * @param appId           the app id
   * @param orchestrationWorkflowId the orchestration id
   * @return the rest response
   */
  @GET
  @Path("{orchestrationWorkflowId}")
  public RestResponse<OrchestrationWorkflow> read(@QueryParam("appId") String appId,
      @PathParam("orchestrationId") String orchestrationWorkflowId, @QueryParam("version") Integer version) {
    return new RestResponse<>(workflowService.readOrchestrationWorkflow(appId, orchestrationWorkflowId));
  }

  /**
   * Creates the.
   *
   * @param appId         the app id
   * @param orchestrationWorkflow the orchestration
   * @return the rest response
   */
  @POST
  public RestResponse<OrchestrationWorkflow> create(
      @QueryParam("appId") String appId, OrchestrationWorkflow orchestrationWorkflow) {
    orchestrationWorkflow.setAppId(appId);
    return new RestResponse<>(workflowService.createOrchestrationWorkflow(orchestrationWorkflow));
  }

  /**
   * Delete.
   *
   * @param appId           the app id
   * @param orchestrationWorkflowId the orchestration id
   * @return the rest response
   */
  @DELETE
  @Path("{orchestrationWorkflowId}")
  public RestResponse delete(
      @QueryParam("appId") String appId, @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId) {
    workflowService.deleteOrchestrationWorkflow(appId, orchestrationWorkflowId);
    return new RestResponse();
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param orchestrationWorkflowId the orchestration id
   * @param workflowOuterSteps   the pre-deployment steps
   * @return the rest response
   */
  @PUT
  @Path("{orchestrationWorkflowId}/pre-deploy")
  public RestResponse<WorkflowOuterSteps> updatePreDeployment(@QueryParam("appId") String appId,
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId, WorkflowOuterSteps workflowOuterSteps) {
    return new RestResponse<>(workflowService.updatePreDeployment(appId, orchestrationWorkflowId, workflowOuterSteps));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param orchestrationWorkflowId the orchestration id
   * @param workflowOuterSteps   the pre-deployment steps
   * @return the rest response
   */
  @PUT
  @Path("{orchestrationWorkflowId}/post-deploy")
  public RestResponse<WorkflowOuterSteps> updatePostDeployment(@QueryParam("appId") String appId,
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId, WorkflowOuterSteps workflowOuterSteps) {
    return new RestResponse<>(workflowService.updatePostDeployment(appId, orchestrationWorkflowId, workflowOuterSteps));
  }

  /**
   * Creates the phase.
   *
   * @param appId         the app id
   * @param orchestrationWorkflowId the orchestration id
   * @param workflowPhase the phase
   * @return the rest response
   */
  @POST
  @Path("{orchestrationWorkflowId}/phases")
  public RestResponse<WorkflowPhase> create(@QueryParam("appId") String appId,
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId, WorkflowPhase workflowPhase) {
    return new RestResponse<>(workflowService.createWorkflowPhase(appId, orchestrationWorkflowId, workflowPhase));
  }

  /**
   * Updates the phase.
   *
   * @param appId         the app id
   * @param orchestrationWorkflowId the orchestration id
   * @param phaseId the orchestration id
   * @param workflowPhase the phase
   * @return the rest response
   */
  @PUT
  @Path("{orchestrationWorkflowId}/phases/{phaseId}")
  public RestResponse<WorkflowPhase> create(@QueryParam("appId") String appId,
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId, @PathParam("phaseId") String phaseId,
      WorkflowPhase workflowPhase) {
    return new RestResponse<>(workflowService.updateWorkflowPhase(appId, orchestrationWorkflowId, workflowPhase));
  }

  /**
   * Delete.
   *
   * @param appId           the app id
   * @param orchestrationWorkflowId the orchestration id
   * @param phaseId the orchestration id
   * @return the rest response
   */
  @DELETE
  @Path("{orchestrationWorkflowId}/phases/{phaseId}")
  public RestResponse<WorkflowPhase> deletePhase(@QueryParam("appId") String appId,
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId, @PathParam("phaseId") String phaseId) {
    workflowService.deleteWorkflowPhase(appId, orchestrationWorkflowId, phaseId);
    return new RestResponse();
  }
}
