/**
 *
 */

package software.wings.resources;

import io.swagger.annotations.Api;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Graph.Node;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.RestResponse;
import software.wings.beans.Variable;
import software.wings.beans.PhaseStep;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.WorkflowService;

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
  public RestResponse<PageResponse<OrchestrationWorkflow>> list(@QueryParam("appId") String appId,
      @BeanParam PageRequest<OrchestrationWorkflow> pageRequest,
      @QueryParam("previousExecutionsCount") Integer previousExecutionsCount) {
    PageResponse<OrchestrationWorkflow> workflows =
        workflowService.listOrchestrationWorkflows(pageRequest, previousExecutionsCount);
    return new RestResponse<>(workflows);
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
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId, @QueryParam("version") Integer version) {
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
   * @param orchestrationWorkflow   the orchestrationWorkflow
   * @return the rest response
   */
  @PUT
  @Path("{orchestrationWorkflowId}/basic")
  public RestResponse<OrchestrationWorkflow> updatePreDeployment(@QueryParam("appId") String appId,
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId,
      OrchestrationWorkflow orchestrationWorkflow) {
    return new RestResponse<>(
        workflowService.updateOrchestrationWorkflowBasic(appId, orchestrationWorkflowId, orchestrationWorkflow));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param orchestrationWorkflowId the orchestration id
   * @param phaseStep   the pre-deployment steps
   * @return the rest response
   */
  @PUT
  @Path("{orchestrationWorkflowId}/pre-deploy")
  public RestResponse<PhaseStep> updatePreDeployment(@QueryParam("appId") String appId,
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId, PhaseStep phaseStep) {
    return new RestResponse<>(workflowService.updatePreDeployment(appId, orchestrationWorkflowId, phaseStep));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param orchestrationWorkflowId the orchestration id
   * @param phaseStep   the pre-deployment steps
   * @return the rest response
   */
  @PUT
  @Path("{orchestrationWorkflowId}/post-deploy")
  public RestResponse<PhaseStep> updatePostDeployment(@QueryParam("appId") String appId,
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId, PhaseStep phaseStep) {
    return new RestResponse<>(workflowService.updatePostDeployment(appId, orchestrationWorkflowId, phaseStep));
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

  /**
   * Updates the GraphNode.
   *
   * @param appId         the app id
   * @param orchestrationWorkflowId the orchestration id
   * @param nodeId the nodeId
   * @param node the node
   * @return the rest response
   */
  @PUT
  @Path("{orchestrationWorkflowId}/nodes/{nodeId}")
  public RestResponse<Node> updateGraphNode(@QueryParam("appId") String appId,
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId,
      @QueryParam("subworkflowId") String subworkflowId, @PathParam("nodeId") String nodeId, Node node) {
    node.setId(nodeId);
    return new RestResponse<>(workflowService.updateGraphNode(appId, orchestrationWorkflowId, subworkflowId, node));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param orchestrationWorkflowId the orchestration id
   * @param notificationRules   the notificationRules
   * @return the rest response
   */
  @PUT
  @Path("{orchestrationWorkflowId}/notification-rules")
  public RestResponse<List<NotificationRule>> updateNotificationRules(@QueryParam("appId") String appId,
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId, List<NotificationRule> notificationRules) {
    return new RestResponse<>(
        workflowService.updateNotificationRules(appId, orchestrationWorkflowId, notificationRules));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param orchestrationWorkflowId the orchestration id
   * @param failureStrategies   the failureStrategies
   * @return the rest response
   */
  @PUT
  @Path("{orchestrationWorkflowId}/failure-strategies")
  public RestResponse<List<FailureStrategy>> updateFailureStrategies(@QueryParam("appId") String appId,
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId, List<FailureStrategy> failureStrategies) {
    return new RestResponse<>(
        workflowService.updateFailureStrategies(appId, orchestrationWorkflowId, failureStrategies));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param orchestrationWorkflowId the orchestration id
   * @param userVariables   the user variables
   * @return the rest response
   */
  @PUT
  @Path("{orchestrationWorkflowId}/user-variables")
  public RestResponse<List<Variable>> updateUserVariables(@QueryParam("appId") String appId,
      @PathParam("orchestrationWorkflowId") String orchestrationWorkflowId, List<Variable> userVariables) {
    return new RestResponse<>(workflowService.updateUserVariables(appId, orchestrationWorkflowId, userVariables));
  }
}
