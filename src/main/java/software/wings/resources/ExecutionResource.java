package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.Deployment;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.DeploymentService;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

// TODO: Auto-generated Javadoc

/**
 * The Class ExecutionResource.
 */
@Path("/executions")
public class ExecutionResource {
  private DeploymentService deploymentService;

  /**
   * Instantiates a new execution resource.
   *
   * @param deploymentService the deployment service
   */
  @Inject
  public ExecutionResource(DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
  }

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Path("deploy/{appId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<PageResponse<Deployment>> list(@BeanParam PageRequest<Deployment> pageRequest) {
    return new RestResponse<PageResponse<Deployment>>(deploymentService.list(pageRequest));
  }

  /**
   * Save.
   *
   * @param deployment the deployment
   * @return the rest response
   */
  @POST
  @Path("deploy/{appId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Deployment> save(Deployment deployment) {
    return new RestResponse<Deployment>(deploymentService.create(deployment));
  }
}
