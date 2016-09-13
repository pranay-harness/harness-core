package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Infra;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.InfraService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * The Class InfraResource.
 */
@Api("infrastructures")
@Path("/infrastructures")
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class InfraResource {
  @Inject private InfraService infraService;

  /**
   * List.
   *
   * @param appId       the app id
   * @param envId       the env id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<Infra>> list(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @BeanParam PageRequest<Infra> pageRequest) {
    pageRequest.addFilter("envId", envId, EQ);
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(infraService.list(pageRequest));
  }

  /**
   * Save.
   *
   * @param appId the app id
   * @param envId the env id
   * @param infra the infra
   * @return the rest response
   */
  @POST
  public RestResponse<Infra> save(@QueryParam("appId") String appId, @QueryParam("envId") String envId, Infra infra) {
    infra.setAppId(appId);
    infra.setEnvId(envId);
    return new RestResponse<>(infraService.save(infra));
  }

  /**
   * Delete.
   *
   * @param infraId the infra id
   * @param appId   the app id
   * @param envId   the env id
   * @return the rest response
   */
  @DELETE
  public RestResponse delete(
      @PathParam("infraId") String infraId, @QueryParam("appId") String appId, @QueryParam("envId") String envId) {
    infraService.delete(appId, envId, infraId);
    return new RestResponse();
  }
}
