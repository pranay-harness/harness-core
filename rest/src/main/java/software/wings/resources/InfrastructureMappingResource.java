package software.wings.resources;

import com.google.inject.Inject;

import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.HostValidationRequest;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.InfrastructureMappingService;

import java.util.List;
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
@Api("infrastructure-mappings")
@Path("infrastructure-mappings")
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
  public RestResponse<InfrastructureMapping> save(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      InfrastructureMapping infrastructureMapping) {
    infrastructureMapping.setAppId(appId);
    infrastructureMapping.setEnvId(envId);
    return new RestResponse<>(infrastructureMappingService.save(infrastructureMapping));
  }

  @GET
  @Path("{infraMappingId}")
  public RestResponse<InfrastructureMapping> get(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("infraMappingId") String infraMappingId) {
    return new RestResponse<>(infrastructureMappingService.get(appId, infraMappingId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/hosts")
  public RestResponse<List<String>> listComputeProviderHosts(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @QueryParam("serviceId") String serviceId,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(
        infrastructureMappingService.listComputeProviderHosts(appId, envId, serviceId, computeProviderId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/launchconfigs")
  public RestResponse<List<LaunchConfiguration>> listLaunchConfigs(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @QueryParam("serviceId") String serviceId,
      @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(
        infrastructureMappingService.listLaunchConfigs(appId, envId, serviceId, computeProviderId));
  }

  @PUT
  @Path("{infraMappingId}")
  public RestResponse<InfrastructureMapping> update(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("infraMappingId") String infraMappingId,
      InfrastructureMapping infrastructureMapping) {
    infrastructureMapping.setAppId(appId);
    infrastructureMapping.setEnvId(envId);
    infrastructureMapping.setUuid(infraMappingId);
    return new RestResponse<>(infrastructureMappingService.update(infrastructureMapping));
  }

  @DELETE
  @Path("{infraMappingId}")
  public RestResponse delete(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("infraMappingId") String infraMappingId) {
    infrastructureMappingService.delete(appId, envId, infraMappingId);
    return new RestResponse();
  }

  @GET
  @Path("stencils")
  public RestResponse<Map<String, Map<String, Object>>> infrastructureMappingSchema(@QueryParam("appId") String appId) {
    return new RestResponse<>(infrastructureMappingService.getInfraMappingStencils(appId));
  }

  @GET
  @Path("infra-types")
  public RestResponse<Map<String, Map<String, String>>> infrastructureTypes(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(infrastructureMappingService.listInfraTypes(appId, envId, serviceId));
  }

  @GET
  @Path("compute-providers/{computeProviderId}/clusters")
  public RestResponse<List<String>> getClusterNames(@QueryParam("appId") String appId,
      @QueryParam("deploymentType") String deploymentType, @PathParam("computeProviderId") String computeProviderId) {
    return new RestResponse<>(infrastructureMappingService.listClusters(appId, deploymentType, computeProviderId));
  }

  @POST
  @Path("validate-hosts")
  public RestResponse<List<HostValidationResponse>> get(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, HostValidationRequest validationRequest) {
    validationRequest.setAppId(appId);
    validationRequest.setEnvId(envId);
    return new RestResponse<>(infrastructureMappingService.validateHost(validationRequest));
  }
}
