/**
 *
 */

package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.ErrorCode.DUPLICATE_STATE_NAMES;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.exception.WingsException.ReportTarget.LOG_SYSTEM;
import static software.wings.security.PermissionAttribute.Action.UPDATE;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.PermissionType.WORKFLOW;
import static software.wings.utils.Validator.validateUuid;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.Api;
import software.wings.api.InstanceElement;
import software.wings.beans.EntityType;
import software.wings.beans.FailureStrategy;
import software.wings.beans.GraphNode;
import software.wings.beans.NotificationRule;
import software.wings.beans.PhaseStep;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.beans.stats.CloneMetadata;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeScope;
import software.wings.stencils.Stencil;

import java.util.List;
import java.util.Map;
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
 * The Class OrchestrationResource.
 *
 * @author Rishi
 */
@Api("workflows")
@Path("/workflows")
@Produces("application/json")
@Scope(ResourceType.APPLICATION)
@AuthRule(permissionType = WORKFLOW)
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
   * @param appId                   the app id
   * @param pageRequest             the page request
   * @param previousExecutionsCount the previous executions count
   * @param workflowTypes           the workflow types
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.WORKFLOW, action = Action.READ)
  public RestResponse<PageResponse<Workflow>> list(@QueryParam("appId") String appId,
      @BeanParam PageRequest<Workflow> pageRequest,
      @QueryParam("previousExecutionsCount") Integer previousExecutionsCount,
      @QueryParam("workflowType") List<String> workflowTypes,
      @QueryParam("details") @DefaultValue("true") boolean details) {
    if ((isEmpty(workflowTypes))
        && (pageRequest.getFilters() == null
               || pageRequest.getFilters().stream().noneMatch(
                      searchFilter -> searchFilter.getFieldName().equals("workflowType")))) {
      pageRequest.addFilter("workflowType", Operator.EQ, WorkflowType.ORCHESTRATION);
    }
    if (!details) {
      return new RestResponse<>(workflowService.listWorkflowsWithoutOrchestration(pageRequest));
    }
    return new RestResponse<>(workflowService.listWorkflows(pageRequest, previousExecutionsCount));
  }

  /**
   *
   * @param minAutoscaleInstances
   * @param maxAutoscaleInstances
   * @param targetCpuUtilizationPercentage
   * @return
   */
  @GET
  @Path("hpa-metric-yaml")
  @Timed
  @ExceptionMetered
  public RestResponse<String> getHPAYamlStringWithCustomMetric(@QueryParam("appId") String appId,
      @QueryParam("minAutoscaleInstances") Integer minAutoscaleInstances,
      @QueryParam("maxAutoscaleInstances") Integer maxAutoscaleInstances,
      @QueryParam("targetCpuUtilizationPercentage") Integer targetCpuUtilizationPercentage) {
    if (minAutoscaleInstances == null) {
      minAutoscaleInstances = Integer.valueOf(0);
    }
    if (maxAutoscaleInstances == null) {
      maxAutoscaleInstances = Integer.valueOf(0);
    }
    if (targetCpuUtilizationPercentage == null) {
      targetCpuUtilizationPercentage = Integer.valueOf(80);
    }
    return new RestResponse<>(workflowService.getHPAYamlStringWithCustomMetric(
        minAutoscaleInstances, maxAutoscaleInstances, targetCpuUtilizationPercentage));
  }

  /**
   * Read.
   *
   * @param appId      the app id
   * @param workflowId the workflow id
   * @param version    the version
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
   * Creates a workflow
   *
   * @param appId    the app id
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

  @PUT
  @Path("{workflowId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Workflow> update(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId, WorkflowVersion workflowVersion) {
    return new RestResponse<>(workflowService.updateWorkflow(appId, workflowId, workflowVersion.getDefaultVersion()));
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class WorkflowVersion {
    private Integer defaultVersion;

    public Integer getDefaultVersion() {
      return defaultVersion;
    }

    public void setDefaultVersion(Integer defaultVersion) {
      this.defaultVersion = defaultVersion;
    }
  }

  /**
   * Delete.
   *
   * @param appId      the app id
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
   * @param appId      the app id
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
   * Clone workflow rest response.
   *
   * @param appId      the app id
   * @param workflowId the workflow id
   * @param cloneMetadata   the workflow
   * @return the rest response
   */
  @POST
  @Path("{workflowId}/clone")
  @Timed
  @ExceptionMetered
  public RestResponse<Workflow> cloneWorkflow(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId, CloneMetadata cloneMetadata) {
    return new RestResponse<>(workflowService.cloneWorkflow(appId, workflowId, cloneMetadata));
  }

  /**
   * Update.
   *
   * @param appId      the app id
   * @param workflowId the orchestration id
   * @param phaseStep  the pre-deployment steps
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
   * @param appId      the app id
   * @param workflowId the orchestration id
   * @param phaseStep  the pre-deployment steps
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
   * @param workflowId    the orchestration id
   * @param workflowPhase the phase
   * @return the rest response
   */
  @POST
  @Path("{workflowId}/phases")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = WORKFLOW, action = UPDATE)
  public RestResponse<WorkflowPhase> create(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId, WorkflowPhase workflowPhase) {
    return new RestResponse<>(workflowService.createWorkflowPhase(appId, workflowId, workflowPhase));
  }

  /**
   * Creates the phase.
   *
   * @param appId         the app id
   * @param workflowId    the orchestration id
   * @param workflowPhase the phase
   * @return the rest response
   */
  @POST
  @Path("{workflowId}/phases/clone")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = WORKFLOW, action = UPDATE)
  public RestResponse<WorkflowPhase> clone(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId, WorkflowPhase workflowPhase) {
    return new RestResponse<>(workflowService.cloneWorkflowPhase(appId, workflowId, workflowPhase));
  }

  /**
   * Updates the phase.
   *
   * @param appId         the app id
   * @param workflowId    the orchestration id
   * @param phaseId       the orchestration id
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

    try {
      return new RestResponse<>(workflowService.updateWorkflowPhase(appId, workflowId, workflowPhase));
    } catch (WingsException exception) {
      // When the workflow update is coming from the user there is no harness engineer wrong doing to alerted for
      exception.excludeReportTarget(DUPLICATE_STATE_NAMES, LOG_SYSTEM);
      exception.excludeReportTarget(INVALID_ARGUMENT, LOG_SYSTEM);
      throw exception;
    }
  }

  /**
   * Updates the phase.
   *
   * @param appId                 the app id
   * @param workflowId            the orchestration id
   * @param phaseId               the orchestration id
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
   * @param appId      the app id
   * @param workflowId the orchestration id
   * @param phaseId    the orchestration id
   * @return the rest response
   */
  @DELETE
  @Path("{workflowId}/phases/{phaseId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = WORKFLOW, action = UPDATE)
  public RestResponse<WorkflowPhase> deletePhase(@QueryParam("appId") String appId,
      @PathParam("workflowId") String workflowId, @PathParam("phaseId") String phaseId) {
    workflowService.deleteWorkflowPhase(appId, workflowId, phaseId);
    return new RestResponse();
  }

  /**
   * Updates the GraphNode.
   *
   * @param appId         the app id
   * @param workflowId    the orchestration id
   * @param subworkflowId the subworkflow id
   * @param nodeId        the nodeId
   * @param node          the node
   * @return the rest response
   */
  @PUT
  @Path("{workflowId}/nodes/{nodeId}")
  @Timed
  @ExceptionMetered
  public RestResponse<GraphNode> updateGraphNode(@QueryParam("appId") String appId,
      @PathParam("workflowId") String workflowId, @QueryParam("subworkflowId") String subworkflowId,
      @PathParam("nodeId") String nodeId, GraphNode node) {
    node.setId(nodeId);
    return new RestResponse<>(workflowService.updateGraphNode(appId, workflowId, subworkflowId, node));
  }

  /**
   * Updates the GraphNode.
   *
   * @param appId         the app id
   * @param workflowId    the orchestration id
   * @param nodeId        the nodeId
   * @return the rest response
   */
  @GET
  @Path("{workflowId}/nodes/{nodeId}")
  @Timed
  @ExceptionMetered
  public RestResponse<GraphNode> readGraphNode(@QueryParam("appId") String appId,
      @PathParam("workflowId") String workflowId, @PathParam("nodeId") String nodeId) {
    return new RestResponse<>(workflowService.readGraphNode(appId, workflowId, nodeId));
  }

  /**
   * Update.
   *
   * @param appId             the app id
   * @param workflowId        the orchestration id
   * @param notificationRules the notificationRules
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
   * @param appId             the app id
   * @param workflowId        the orchestration id
   * @param failureStrategies the failureStrategies
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
   * @param appId         the app id
   * @param workflowId    the orchestration id
   * @param userVariables the user variables
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
   * @param appId      the app id
   * @param envId      the env id
   * @param workflowId the workflow id
   * @param phaseId    the phase id
   * @return the rest response
   */
  @GET
  @Path("stencils")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<List<Stencil>> stencils(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @QueryParam("workflowId") String workflowId, @QueryParam("phaseId") String phaseId) {
    return new RestResponse<>(
        workflowService.stencils(appId, workflowId, phaseId, StateTypeScope.ORCHESTRATION_STENCILS)
            .get(StateTypeScope.ORCHESTRATION_STENCILS));
  }

  /**
   * Stencils rest response.
   *
   * @param appId      the app id
   * @param serviceId the workflow id
   * @param strStateType    the state type
   * @return the rest response
   */
  @GET
  @Path("state-defaults")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> stateDefaults(@QueryParam("appId") String appId,
      @QueryParam("serviceId") String serviceId, @QueryParam("stateType") String strStateType) {
    if (isEmpty(strStateType)) {
      return new RestResponse<>();
    }
    return new RestResponse<>(workflowService.getStateDefaults(appId, serviceId, StateType.valueOf(strStateType)));
  }

  @GET
  @Path("{workflowId}/infra-types")
  @Timed
  public RestResponse<Boolean> workflowHasSSHInfraMapping(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId) {
    return new RestResponse(workflowService.workflowHasSshInfraMapping(appId, workflowId));
  }

  @GET
  @Path("{workflowId}/deployed-nodes")
  @Timed
  @ExceptionMetered
  public RestResponse<List<InstanceElement>> getDeployedNodes(
      @QueryParam("appId") String appId, @PathParam("workflowId") String workflowId) {
    return new RestResponse<>(workflowService.getDeployedNodes(appId, workflowId));
  }

  @GET
  @Path("required-entities")
  @Timed
  @ExceptionMetered
  public RestResponse<List<EntityType>> requiredEntities(
      @QueryParam("appId") String appId, @QueryParam("workflowId") String workflowId) {
    return new RestResponse<>(workflowService.getRequiredEntities(appId, workflowId));
  }
}
