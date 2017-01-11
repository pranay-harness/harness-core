package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.InfrastructureMappingService;

import java.util.Map;
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
 * Created by anubhaw on 1/10/17.
 */
@Api("infrastructure-mapping")
@Path("/infrastructure-mapping")
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class InfrastructureMappingResource {
  @Inject private InfrastructureMappingService infrastructureMappingService;

  @GET
  public RestResponse<PageResponse<InfrastructureMapping>> list(
      @BeanParam PageRequest<InfrastructureMapping> pageRequest) {
    return new RestResponse<>(infrastructureMappingService.list(pageRequest));
  }

  @POST
  public RestResponse<InfrastructureMapping> save(
      @QueryParam("appId") String appId, InfrastructureMapping infrastructureMapping) {
    infrastructureMapping.setAppId(appId);
    return new RestResponse<>(infrastructureMappingService.save(infrastructureMapping));
  }

  @GET
  @Path("{infraMappingId}")
  public RestResponse<InfrastructureMapping> get(
      @QueryParam("appId") String appId, @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.get(appId, infraMappingId));
  }

  @PUT
  @Path("{infraMappingId}")
  public RestResponse<InfrastructureMapping> update(@QueryParam("appId") String appId,
      @PathParam("infraMappingId") String infraMappingId, InfrastructureMapping infrastructureMapping) {
    infrastructureMapping.setAppId(appId);
    infrastructureMapping.setUuid(infraMappingId);
    return new RestResponse<>(infrastructureMappingService.update(infrastructureMapping));
  }

  @DELETE
  @Path("{infraMappingId}")
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("infraMappingId") String infraMappingId) {
    infrastructureMappingService.delete(appId, infraMappingId);
    return new RestResponse();
  }

  @GET
  @Path("stencils")
  public RestResponse<Map<String, Map<String, Object>>> infrastructureMappingSchema(@QueryParam("appId") String appId) {
    return new RestResponse<>(infrastructureMappingService.getInfraMappingStencils(appId));
  }
}
