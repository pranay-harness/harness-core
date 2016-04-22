package software.wings.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import software.wings.beans.Deployment;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.DeploymentService;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/executions")
public class ExecutionResource {
  private static final Logger logger = LoggerFactory.getLogger(ExecutionResource.class);

  private DeploymentService deploymentService;

  @Inject
  public ExecutionResource(DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
  }

  @GET
  @Path("deploy/{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<PageResponse<Deployment>> list(@BeanParam PageRequest<Deployment> pageRequest) {
    return new RestResponse<PageResponse<Deployment>>(deploymentService.list(pageRequest));
  }

  @POST
  @Path("deploy/{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Deployment> save(Deployment deployment) {
    return new RestResponse<Deployment>(deploymentService.create(deployment));
  }
}
