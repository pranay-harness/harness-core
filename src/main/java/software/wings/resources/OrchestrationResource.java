/**
 *
 */
package software.wings.resources;

import software.wings.beans.Orchestration;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.service.intfc.WorkflowService;

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
@Path("/Orchestrations")
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
  @Path("playbacks")
  @Produces("application/json")
  public RestResponse<PageResponse<Orchestration>> listPlaybacks(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @BeanParam PageRequest<Orchestration> pageRequest) {
    return null;
  }

  @GET
  @Path("{orchestrationId}")
  @Produces("application/json")
  public RestResponse<Orchestration> read(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("orchestrationId") String orchestrationId) {
    return new RestResponse<Orchestration>(workflowService.readOrchestration(appId, orchestrationId));
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
}
