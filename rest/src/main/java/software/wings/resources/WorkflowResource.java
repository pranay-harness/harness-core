/**
 *
 */

package software.wings.resources;

import static software.wings.utils.Validator.validateUuid;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Graph.Node;
import software.wings.beans.NotificationRule;
import software.wings.beans.PhaseStep;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
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
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Workflow>> list(@QueryParam("appId") String appId,
      @BeanParam PageRequest<Workflow> pageRequest,
      @QueryParam("previousExecutionsCount") Integer previousExecutionsCount,
      @QueryParam("workflowType") List<String> workflowTypes) {
    if ((workflowTypes == null || workflowTypes.isEmpty())
        && (pageRequest.getFilters() == null
               || pageRequest.getFilters().stream().noneMatch(
                      searchFilter -> searchFilter.getFieldName().equals("workflowType")))) {
      pageRequest.addFilter("workflowType", WorkflowType.ORCHESTRATION, Operator.EQ);
    }
    PageResponse<Workflow> workflows = workflowService.listWorkflows(pageRequest, previousExecutionsCount);
    return new RestResponse<>(workflows);
  }

  /**
   * Read.
   *
   * @param appId           the app id
   * @param workflowId the workflow id
   * @return the rest response
   */
  @GET
  @Path("{workflowId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Workflow> read(@QueryParam("appId") String appId, @PathParam("workflowId") String workflowId,
      @QueryParam("version") Integer version) {
    return new RestResponse<>(workflowService.readWorkflow(appId, workflowId, version));
  }

  /**
   * Creates the.
   *
   * @param appId         the app id
   * @param workflow the workflow
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Workflow> create(@QueryParam("appId") String appId, Workflow workflow) {
    workflow.setAppId(appId);
    workflow.setWorkflowType(WorkflowType.ORCHESTRATION);
    return new RestResponse<>(workflowService.createWorkflow(workflow));
  }

  /**
   * Delete.
   *
   * @param appId           the app id
   * @param workflowId the orchestration id
   * @return the rest response
   */
  @DELETE
  @Path("{workflowId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("workflowId") String workflowId) {
    workflowService.deleteWorkflow(appId, workflowId);
    return new RestResponse();
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param workflowId the workflow id
   * @param workflow   the workflow
   * @return the rest response
   */
  @PUT
  @Path("{workflowId}/basic")
  @Timed
  @ExceptionMetered
  public RestResponse<Workflow> updatePreDeployment(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId, Workflow workflow) {
    validateUuid(workflow, "workflowId", workflowId);
    workflow.setAppId(appId);
    return new RestResponse<>(workflowService.updateWorkflow(workflow, null));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param workflowId the orchestration id
   * @param phaseStep   the pre-deployment steps
   * @return the rest response
   */
  @PUT
  @Path("{workflowId}/pre-deploy")
  @Timed
  @ExceptionMetered
  public RestResponse<PhaseStep> updatePreDeployment(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId, PhaseStep phaseStep) {
    return new RestResponse<>(workflowService.updatePreDeployment(appId, workflowId, phaseStep));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param workflowId the orchestration id
   * @param phaseStep   the pre-deployment steps
   * @return the rest response
   */
  @PUT
  @Path("{workflowId}/post-deploy")
  @Timed
  @ExceptionMetered
  public RestResponse<PhaseStep> updatePostDeployment(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId, PhaseStep phaseStep) {
    return new RestResponse<>(workflowService.updatePostDeployment(appId, workflowId, phaseStep));
  }

  /**
   * Creates the phase.
   *
   * @param appId         the app id
   * @param workflowId the orchestration id
   * @param workflowPhase the phase
   * @return the rest response
   */
  @POST
  @Path("{workflowId}/phases")
  @Timed
  @ExceptionMetered
  public RestResponse<WorkflowPhase> create(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId, WorkflowPhase workflowPhase) {
    return new RestResponse<>(workflowService.createWorkflowPhase(appId, workflowId, workflowPhase));
  }

  /**
   * Updates the phase.
   *
   * @param appId         the app id
   * @param workflowId the orchestration id
   * @param phaseId the orchestration id
   * @param workflowPhase the phase
   * @return the rest response
   */
  @PUT
  @Path("{workflowId}/phases/{phaseId}")
  @Timed
  @ExceptionMetered
  public RestResponse<WorkflowPhase> update(@QueryParam("appId") String appId,
      @PathParam("workflowId") String workflowId, @PathParam("phaseId") String phaseId, WorkflowPhase workflowPhase) {
    validateUuid(workflowPhase, "phaseId", phaseId);
    return new RestResponse<>(workflowService.updateWorkflowPhase(appId, workflowId, workflowPhase));
  }

  /**
   * Updates the phase.
   *
   * @param appId         the app id
   * @param workflowId the orchestration id
   * @param phaseId the orchestration id
   * @param rollbackWorkflowPhase the rollback workflow phase
   * @return the rest response
   */
  @PUT
  @Path("{workflowId}/phases/{phaseId}/rollback")
  @Timed
  @ExceptionMetered
  public RestResponse<WorkflowPhase> updateRollback(@QueryParam("appId") String appId,
      @PathParam("workflowId") String workflowId, @PathParam("phaseId") String phaseId,
      WorkflowPhase rollbackWorkflowPhase) {
    return new RestResponse<>(
        workflowService.updateWorkflowPhaseRollback(appId, workflowId, phaseId, rollbackWorkflowPhase));
  }

  /**
   * Delete.
   *
   * @param appId           the app id
   * @param workflowId the orchestration id
   * @param phaseId the orchestration id
   * @return the rest response
   */
  @DELETE
  @Path("{workflowId}/phases/{phaseId}")
  @Timed
  @ExceptionMetered
  public RestResponse<WorkflowPhase> deletePhase(@QueryParam("appId") String appId,
      @PathParam("workflowId") String workflowId, @PathParam("phaseId") String phaseId) {
    workflowService.deleteWorkflowPhase(appId, workflowId, phaseId);
    return new RestResponse();
  }

  /**
   * Updates the GraphNode.
   *
   * @param appId         the app id
   * @param workflowId the orchestration id
   * @param nodeId the nodeId
   * @param node the node
   * @return the rest response
   */
  @PUT
  @Path("{workflowId}/nodes/{nodeId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Node> updateGraphNode(@QueryParam("appId") String appId,
      @PathParam("workflowId") String workflowId, @QueryParam("subworkflowId") String subworkflowId,
      @PathParam("nodeId") String nodeId, Node node) {
    node.setId(nodeId);
    return new RestResponse<>(workflowService.updateGraphNode(appId, workflowId, subworkflowId, node));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param workflowId the orchestration id
   * @param notificationRules   the notificationRules
   * @return the rest response
   */
  @PUT
  @Path("{workflowId}/notification-rules")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NotificationRule>> updateNotificationRules(@QueryParam("appId") String appId,
      @PathParam("workflowId") String workflowId, List<NotificationRule> notificationRules) {
    return new RestResponse<>(workflowService.updateNotificationRules(appId, workflowId, notificationRules));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param workflowId the orchestration id
   * @param failureStrategies   the failureStrategies
   * @return the rest response
   */
  @PUT
  @Path("{workflowId}/failure-strategies")
  @Timed
  @ExceptionMetered
  public RestResponse<List<FailureStrategy>> updateFailureStrategies(@QueryParam("appId") String appId,
      @PathParam("workflowId") String workflowId, List<FailureStrategy> failureStrategies) {
    return new RestResponse<>(workflowService.updateFailureStrategies(appId, workflowId, failureStrategies));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param workflowId the orchestration id
   * @param userVariables   the user variables
   * @return the rest response
   */
  @PUT
  @Path("{workflowId}/user-variables")
  @Timed
  @ExceptionMetered
  public RestResponse<List<Variable>> updateUserVariables(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId, List<Variable> userVariables) {
    return new RestResponse<>(workflowService.updateUserVariables(appId, workflowId, userVariables));
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
