/**
 *
 */
package software.wings.resources;

import software.wings.beans.Orchestration;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.WorkflowService;

import java.util.List;

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
 *
 */
@Path("/orchestrations")
public class OrchestrationResource {
  private WorkflowService workflowService;

  @Inject
  public OrchestrationResource(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @GET
  @Produces("application/json")
  public RestResponse<PageResponse<Orchestration>> list(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @BeanParam PageRequest<Orchestration> pageRequest) {
    pageRequest.addFilter("appId", appId, SearchFilter.Operator.EQ);
    pageRequest.addFilter("environment.uuid", envId, SearchFilter.Operator.EQ);
    return new RestResponse<PageResponse<Orchestration>>(workflowService.listOrchestration(pageRequest));
  }

  @GET
  @Path("{orchestrationId}")
  @Produces("application/json")
  public RestResponse<Orchestration> read(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("orchestrationId") String orchestrationId) {
    return new RestResponse<Orchestration>(workflowService.readOrchestration(appId, envId, orchestrationId));
  }

  @POST
  @Produces("application/json")
  public RestResponse<Orchestration> create(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, Orchestration orchestration) {
    orchestration.setAppId(appId);
    return new RestResponse<Orchestration>(workflowService.createWorkflow(Orchestration.class, orchestration));
  }

  @PUT
  @Path("{orchestrationId}")
  @Produces("application/json")
  public RestResponse<Orchestration> update(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("orchestrationId") String orchestrationId, Orchestration orchestration) {
    orchestration.setAppId(appId);
    return new RestResponse<Orchestration>(workflowService.updateWorkflow(Orchestration.class, orchestration));
  }

  @GET
  @Path("{orchestrationId}/executions")
  @Produces("application/json")
  public RestResponse<PageResponse<WorkflowExecution>> listExecutions(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("orchestrationId") String orchestrationId,
      @BeanParam PageRequest<WorkflowExecution> pageRequest) {
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValue(appId);
    filter.setOp(Operator.EQ);
    pageRequest.getFilters().add(filter);

    filter = new SearchFilter();
    filter.setFieldName("workflowExecutionType");
    filter.setFieldValue(WorkflowExecutionType.ORCHESTRATION);
    filter.setOp(Operator.EQ);
    pageRequest.getFilters().add(filter);

    return new RestResponse<PageResponse<WorkflowExecution>>(workflowService.listExecutions(pageRequest, false));
  }

  @GET
  @Path("executions/{workflowExecutionId}")
  @Produces("application/json")
  public RestResponse<WorkflowExecution> getExecutionDetails(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("workflowExecutionId") String workflowExecutionId) {
    return new RestResponse<WorkflowExecution>(workflowService.getExecutionDetails(appId, workflowExecutionId));
  }

  @POST
  @Path("{orchestrationId}/executions")
  @Produces("application/json")
  public RestResponse<WorkflowExecution> triggerExecution(@QueryParam("appId") String appId,
      @PathParam("orchestrationId") String orchestrationId, @QueryParam("artifactId") List<String> artifactIds) {
    return new RestResponse<WorkflowExecution>(
        workflowService.triggerOrchestrationExecution(appId, orchestrationId, artifactIds));
  }

  @PUT
  @Path("executions/{workflowExecutionId}")
  @Produces("application/json")
  public RestResponse<WorkflowExecution> updateExecutionDetails(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("workflowExecutionId") String workflowExecutionId) {
    // TODO - implement abort and pause functionality
    return null;
  }
}
