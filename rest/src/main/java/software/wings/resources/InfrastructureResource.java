package software.wings.resources;

import static software.wings.beans.SearchFilter.Builder.aSearchFilter;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.InfrastructureService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
@PublicApi // TODO:: remove
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

  /**
   * List infra host rest response.
   *
   * @param infraId     the infra id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Path("{infraId}/hosts")
  public RestResponse<PageRequest<Host>> listInfraHost(
      @PathParam("infraId") String infraId, @BeanParam PageRequest<Host> pageRequest) {
    pageRequest.addFilter(aSearchFilter().withField("infraId", Operator.EQ, infraId).build());
    return new RestResponse<>(infrastructureService.listInfraHost(pageRequest));
  }

  /**
   * Save.
   *
   * @param infrastructure the infrastructure
   * @return the rest response
   */
  @POST
  public RestResponse<Infrastructure> save(Infrastructure infrastructure) {
    return new RestResponse<>(infrastructureService.save(infrastructure));
  }

  /**
   * Delete.
   *
   * @param infraId the infra id
   * @return the rest response
   */
  @DELETE
  @Path("{infraId}")
  public RestResponse delete(@PathParam("infraId") String infraId) {
    infrastructureService.delete(infraId);
    return new RestResponse();
  }

  @POST
  @Path("{infraId}/sync")
  public RestResponse sync(@PathParam("infraId") String infraId) {
    infrastructureService.sync(infraId);
    return new RestResponse();
  }
}
