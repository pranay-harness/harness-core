package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AzureResourceService;

import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("azure")
@Path("/azure")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Scope(SETTING)
public class AzureResource {
  @Inject private AzureResourceService azureResourceService;

  @GET
  @Path("/subscriptions")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> listSubscriptions(
      @QueryParam("accountId") String accountId, @QueryParam("cloudProviderId") String cloudProviderId) {
    return new RestResponse(azureResourceService.listSubscriptions(cloudProviderId));
  }

  @GET
  @Path("/subscriptions/{subscriptionId}/containerRegistries")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listContainerRegistries(@QueryParam("accountId") String accountId,
      @QueryParam("cloudProviderId") String cloudProviderId,
      @PathParam(value = "subscriptionId") String subscriptionId) {
    return new RestResponse(azureResourceService.listContainerRegistries(cloudProviderId, subscriptionId));
  }

  @GET
  @Path("/subscriptions/{subscriptionId}/containerRegistries/{registryName}/repositories")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listRepositories(@QueryParam("accountId") String accountId,
      @QueryParam("cloudProviderId") String cloudProviderId, @PathParam(value = "subscriptionId") String subscriptionId,
      @PathParam(value = "registryName") String registryName) {
    return new RestResponse(azureResourceService.listRepositories(cloudProviderId, subscriptionId, registryName));
  }

  @GET
  @Path("/subscriptions/{subscriptionId}/containerRegistries/{registryName}/repositories/{repositoryName}/tags")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listRepositoryTags(@QueryParam("accountId") String accountId,
      @QueryParam("cloudProviderId") String cloudProviderId, @PathParam(value = "subscriptionId") String subscriptionId,
      @PathParam(value = "registryName") String registryName,
      @PathParam(value = "repositoryName") String repositoryName) {
    return new RestResponse(
        azureResourceService.listRepositoryTags(cloudProviderId, subscriptionId, registryName, repositoryName));
  }

  @GET
  @Path("/subscriptions/{subscriptionId}/kubernetesClusters")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AzureKubernetesCluster>> listKubernetesClusters(@QueryParam("accountId") String accountId,
      @QueryParam("cloudProviderId") String cloudProviderId,
      @PathParam(value = "subscriptionId") String subscriptionId) {
    return new RestResponse(azureResourceService.listKubernetesClusters(cloudProviderId, subscriptionId));
  }
}