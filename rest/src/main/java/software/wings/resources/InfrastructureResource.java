package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.InfrastructureService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * The Class InfrastructureResource.
 */
@Api("infrastructures")
@Path("/infrastructures")
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class InfrastructureResource {
  @Inject private InfrastructureService infrastructureService;

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<Infrastructure>> list(@BeanParam PageRequest<Infrastructure> pageRequest,
      @QueryParam("overview") @DefaultValue("false") boolean overview) {
    return new RestResponse<>(infrastructureService.list(pageRequest, overview));
  }
}
