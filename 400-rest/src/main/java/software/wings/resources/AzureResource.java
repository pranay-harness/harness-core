/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import software.wings.beans.AzureContainerRegistry;
import software.wings.beans.AzureImageDefinition;
import software.wings.beans.AzureImageGallery;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.AzureResourceGroup;
import software.wings.beans.NameValuePair;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AzureResourceService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
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
@OwnedBy(CDC)
public class AzureResource {
  @Inject private AzureResourceService azureResourceService;

  @GET
  @Path("/subscriptions")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> listSubscriptions(
      @QueryParam("accountId") String accountId, @QueryParam("cloudProviderId") String cloudProviderId) {
    return new RestResponse(azureResourceService.listSubscriptions(accountId, cloudProviderId));
  }

  @GET
  @Path("/subscriptions/{subscriptionId}/containerRegistries")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listContainerRegistries(@QueryParam("accountId") String accountId,
      @QueryParam("cloudProviderId") String cloudProviderId,
      @PathParam(value = "subscriptionId") String subscriptionId) {
    return new RestResponse(azureResourceService.listContainerRegistryNames(cloudProviderId, subscriptionId));
  }

  @GET
  @Path("/subscriptions/{subscriptionId}/containerRegistriesWithDetails")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AzureContainerRegistry>> listContainerRegistriesWithDetails(
      @QueryParam("accountId") String accountId, @QueryParam("cloudProviderId") String cloudProviderId,
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

  @GET
  @Path("/subscriptions/{subscriptionId}/resourceGroups")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AzureResourceGroup>> listResourceGroups(@QueryParam("accountId") String accountId,
      @QueryParam("cloudProviderId") String cloudProviderId,
      @PathParam(value = "subscriptionId") String subscriptionId) {
    return new RestResponse(azureResourceService.listResourceGroups(accountId, cloudProviderId, subscriptionId));
  }

  @GET
  @Path("/subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/imageGalleries")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AzureImageGallery>> listImageGalleries(@QueryParam("accountId") String accountId,
      @QueryParam("cloudProviderId") String cloudProviderId, @PathParam(value = "subscriptionId") String subscriptionId,
      @PathParam(value = "resourceGroupName") String resourceGroupName) {
    return new RestResponse(
        azureResourceService.listImageGalleries(accountId, cloudProviderId, subscriptionId, resourceGroupName));
  }

  @GET
  @Path(
      "/subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/imageGalleries/{galleryName}/imageDefinitions")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AzureImageDefinition>>
  listImageDefinitions(@QueryParam("accountId") String accountId, @QueryParam("cloudProviderId") String cloudProviderId,
      @PathParam(value = "subscriptionId") String subscriptionId,
      @PathParam(value = "resourceGroupName") String resourceGroupName,
      @PathParam(value = "galleryName") String galleryName) {
    return new RestResponse(azureResourceService.listImageDefinitions(
        accountId, cloudProviderId, subscriptionId, resourceGroupName, galleryName));
  }
  /**
   * List Azure regions.
   *
   * @param accountId
   * @return
   */
  @GET
  @Path("regions")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NameValuePair>> listAzureRegions(@QueryParam("accountId") String accountId) {
    return new RestResponse(azureResourceService.listAzureRegions());
  }
}
