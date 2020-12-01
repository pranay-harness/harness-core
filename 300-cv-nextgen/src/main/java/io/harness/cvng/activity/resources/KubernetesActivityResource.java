package io.harness.cvng.activity.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.KUBERNETES_RESOURCE;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.activity.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api(KUBERNETES_RESOURCE)
@Path(KUBERNETES_RESOURCE)
@Produces("application/json")
@ExposeInternalException
public class KubernetesActivityResource {
  @Inject private KubernetesActivitySourceService kubernetesActivitySourceService;

  @POST
  @Timed
  @ExceptionMetered
  @NextGenManagerAuth
  @Path("/source")
  @ApiOperation(value = "register a kubernetes event source", nickname = "registerKubernetesSource")
  public RestResponse<String> registerKubernetesSource(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @Body KubernetesActivitySourceDTO activitySourceDTO) {
    return new RestResponse<>(kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, activitySourceDTO));
  }

  @GET
  @Timed
  @ExceptionMetered
  @NextGenManagerAuth
  @Path("/source")
  @ApiOperation(value = "gets a kubernetes event source by identifier", nickname = "getKubernetesSource")
  public RestResponse<KubernetesActivitySourceDTO> getKubernetesSource(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("identifier") @NotNull String identifier) {
    return new RestResponse<>(
        kubernetesActivitySourceService.getKubernetesSource(accountId, orgIdentifier, projectIdentifier, identifier));
  }

  @GET
  @Timed
  @ExceptionMetered
  @NextGenManagerAuth
  @Path("/sources")
  @ApiOperation(value = "lists all kubernetes event sources", nickname = "listKubernetesSources")
  public RestResponse<PageResponse<KubernetesActivitySourceDTO>> listKubernetesSources(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @QueryParam("offset") @NotNull Integer offset,
      @QueryParam("pageSize") @NotNull Integer pageSize, @QueryParam("filter") String filter) {
    return new RestResponse<>(kubernetesActivitySourceService.listKubernetesSources(
        accountId, orgIdentifier, projectIdentifier, offset, pageSize, filter));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @NextGenManagerAuth
  @Path("{identifier}")
  @ApiOperation(value = "deletes a kubernetes event source", nickname = "deleteKubernetesSource")
  public RestResponse<Boolean> deleteKubernetesSource(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @PathParam("identifier") @NotNull String identifier) {
    return new RestResponse<>(kubernetesActivitySourceService.deleteKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, identifier));
  }

  @POST
  @Timed
  @ExceptionMetered
  @DelegateAuth
  @Path("/activities")
  @ApiOperation(value = "saves a list of kubernetes activities", nickname = "saveKubernetesActivities")
  public RestResponse<Boolean> saveKubernetesActivities(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("activitySourceId") @NotNull String activitySourceId,
      @NotNull @Valid @Body List<KubernetesActivityDTO> activities) {
    return new RestResponse<>(
        kubernetesActivitySourceService.saveKubernetesActivities(accountId, activitySourceId, activities));
  }

  @GET
  @Timed
  @ExceptionMetered
  @NextGenManagerAuth
  @Path("/namespaces")
  @ApiOperation(value = "gets a list of kubernetes namespaces", nickname = "getNamespaces")
  public RestResponse<PageResponse<String>> getNamespaces(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("connectorIdentifier") @NotNull String connectorIdentifier,
      @QueryParam("offset") @NotNull Integer offset, @QueryParam("pageSize") @NotNull Integer pageSize,
      @QueryParam("filter") String filter) {
    return new RestResponse<>(kubernetesActivitySourceService.getKubernetesNamespaces(
        accountId, orgIdentifier, projectIdentifier, connectorIdentifier, offset, pageSize, filter));
  }

  @GET
  @Timed
  @ExceptionMetered
  @NextGenManagerAuth
  @Path("/workloads")
  @ApiOperation(value = "gets a list of kubernetes workloads", nickname = "getWorkloads")
  public RestResponse<PageResponse<String>> getWorkloads(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("connectorIdentifier") @NotNull String connectorIdentifier,
      @QueryParam("namespace") @NotNull String namespace, @QueryParam("offset") @NotNull Integer offset,
      @QueryParam("pageSize") @NotNull Integer pageSize, @QueryParam("filter") String filter) {
    return new RestResponse<>(kubernetesActivitySourceService.getKubernetesWorkloads(
        accountId, orgIdentifier, projectIdentifier, connectorIdentifier, namespace, offset, pageSize, filter));
  }
}
