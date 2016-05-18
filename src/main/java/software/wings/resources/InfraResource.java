package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.Infra;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.AuthRule;
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

@Path("/infrastructures")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class InfraResource {
  @Inject private InfraService infraService;

  @GET
  public RestResponse<PageResponse<Infra>> list(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @BeanParam PageRequest<Infra> pageRequest) {
    pageRequest.addFilter("envId", envId, EQ);
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(infraService.list(pageRequest));
  }

  @POST
  public RestResponse<Infra> save(@QueryParam("appId") String appId, @QueryParam("envId") String envId, Infra infra) {
    infra.setAppId(appId);
    infra.setEnvId(envId);
    return new RestResponse<>(infraService.save(infra));
  }

  @DELETE
  public RestResponse delete(
      @PathParam("infraId") String infraId, @QueryParam("appId") String appId, @QueryParam("envId") String envId) {
    infraService.delete(infraId);
    return new RestResponse();
  }
}
