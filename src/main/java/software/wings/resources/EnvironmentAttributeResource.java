package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.EnvironmentAttribute;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.EnvironmentAttributeService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 5/17/16.
 */

@Path("/env-attributes")
@Timed
@ExceptionMetered
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class EnvironmentAttributeResource {
  @Inject private EnvironmentAttributeService attributeService;

  @GET
  public RestResponse<PageResponse<EnvironmentAttribute>> list(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @BeanParam PageRequest<EnvironmentAttribute> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    pageRequest.addFilter("envId", envId, EQ);
    return new RestResponse<>(attributeService.list(pageRequest));
  }

  @POST
  public RestResponse<EnvironmentAttribute> save(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, EnvironmentAttribute variable) {
    variable.setAppId(appId);
    variable.setEnvId(envId);
    return new RestResponse<>(attributeService.save(variable));
  }

  @GET
  @Path("{attrId}")
  public RestResponse<EnvironmentAttribute> get(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @PathParam("attrId") String attrId) {
    return new RestResponse<>(attributeService.get(appId, envId, appId));
  }

  @PUT
  @Path("{attrId}")
  public RestResponse<EnvironmentAttribute> update(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("attrId") String attrId, EnvironmentAttribute variable) {
    variable.setUuid(attrId);
    variable.setAppId(appId);
    variable.setEnvId(envId);
    return new RestResponse<>(attributeService.update(variable));
  }

  @DELETE
  @Path("{attrId}")
  public RestResponse delete(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @PathParam("attrId") String attrId) {
    attributeService.delete(appId, envId, attrId);
    return new RestResponse();
  }
}
