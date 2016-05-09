/**
 *
 */
package software.wings.resources;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Pipeline;
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
@Path("/pipelines")
public class PipelineResource {
  private WorkflowService workflowService;

  @Inject
  public PipelineResource(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @GET
  @Produces("application/json")
  public RestResponse<PageResponse<Pipeline>> list(
      @QueryParam("applicationId") String applicationId, @BeanParam PageRequest<Pipeline> pageRequest) {
    pageRequest.addFilter("application", applicationId, SearchFilter.Operator.EQ);
    return new RestResponse<PageResponse<Pipeline>>(workflowService.listPipeline(pageRequest));
  }

  @GET
  @Path("{pipelineId}")
  @Produces("application/json")
  public RestResponse<Pipeline> read(
      @QueryParam("applicationId") String applicationId, @PathParam("pipelineId") String pipelineId) {
    return new RestResponse<Pipeline>(workflowService.readPipeline(applicationId, pipelineId));
  }

  @POST
  @Produces("application/json")
  public RestResponse<Pipeline> create(@QueryParam("applicationId") String applicationId, Pipeline pipeline) {
    pipeline.setApplicationId(applicationId);
    return new RestResponse<Pipeline>(workflowService.createPipeline(pipeline));
  }

  @PUT
  @Path("{pipelineId}")
  @Produces("application/json")
  public RestResponse<Pipeline> update(@QueryParam("applicationId") String applicationId,
      @PathParam("pipelineId") String pipelineId, Pipeline pipeline) {
    pipeline.setApplicationId(applicationId);
    return new RestResponse<Pipeline>(workflowService.updatePipeline(pipeline));
  }
}
