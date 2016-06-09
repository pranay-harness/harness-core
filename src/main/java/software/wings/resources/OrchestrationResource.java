/**
 *
 */

package software.wings.resources;

import io.swagger.annotations.Api;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCodes;
import software.wings.beans.Orchestration;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.service.intfc.EnvironmentService;
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

// TODO: Auto-generated Javadoc

/**
 * The Class OrchestrationResource.
 *
 * @author Rishi
 */
@Api("orchestrations")
@Path("/orchestrations")
public class OrchestrationResource {
  private WorkflowService workflowService;
  private EnvironmentService environmentService;

  /**
   * Instantiates a new orchestration resource.
   *
   * @param workflowService    the workflow service
   * @param environmentService the environment service
   */
  @Inject
  public OrchestrationResource(WorkflowService workflowService, EnvironmentService environmentService) {
    this.workflowService = workflowService;
    this.environmentService = environmentService;
  }

  /**
   * List.
   *
   * @param appId       the app id
   * @param envId       the env id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Produces("application/json")
  public RestResponse<PageResponse<Orchestration>> list(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @BeanParam PageRequest<Orchestration> pageRequest) {
    pageRequest.addFilter("appId", appId, SearchFilter.Operator.EQ);
    Environment env = environmentService.get(appId, envId);
    if (env == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Unknown environment");
    }
    pageRequest.addFilter("environment", env, SearchFilter.Operator.EQ);
    return new RestResponse<>(workflowService.listOrchestration(pageRequest));
  }

  /**
   * Read.
   *
   * @param appId           the app id
   * @param envId           the env id
   * @param orchestrationId the orchestration id
   * @return the rest response
   */
  @GET
  @Path("{orchestrationId}")
  @Produces("application/json")
  public RestResponse<Orchestration> read(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("orchestrationId") String orchestrationId) {
    return new RestResponse<>(workflowService.readOrchestration(appId, envId, orchestrationId));
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
  @Produces("application/json")
  public RestResponse<Orchestration> create(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, Orchestration orchestration) {
    orchestration.setAppId(appId);
    Environment env = environmentService.get(appId, envId);
    if (env == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Unknown environment");
    }
    orchestration.setEnvironment(env);
    return new RestResponse<>(workflowService.createWorkflow(Orchestration.class, orchestration));
  }

  /**
   * Update.
   *
   * @param appId           the app id
   * @param envId           the env id
   * @param orchestrationId the orchestration id
   * @param orchestration   the orchestration
   * @return the rest response
   */
  @PUT
  @Path("{orchestrationId}")
  @Produces("application/json")
  public RestResponse<Orchestration> update(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("orchestrationId") String orchestrationId, Orchestration orchestration) {
    orchestration.setAppId(appId);
    orchestration.setUuid(orchestrationId);
    Environment env = environmentService.get(appId, envId);
    if (env == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Unknown environment");
    }
    orchestration.setEnvironment(env);
    return new RestResponse<>(workflowService.updateOrchestration(orchestration));
  }

  /**
   * Delete.
   *
   * @param appId           the app id
   * @param orchestrationId the orchestration id
   * @param pipeline        the pipeline
   * @return the rest response
   */
  @DELETE
  @Path("{orchestrationId}")
  @Produces("application/json")
  public RestResponse delete(
      @QueryParam("appId") String appId, @PathParam("orchestrationId") String orchestrationId, Pipeline pipeline) {
    workflowService.deleteWorkflow(Orchestration.class, appId, orchestrationId);
    return new RestResponse();
  }
}
