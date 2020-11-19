package io.harness.cvng.activity.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.KUBERNETES_RESOURCE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.activity.beans.KubernetesActivitySourceDTO;
import io.harness.cvng.activity.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.KubernetesActivityDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.harness.security.annotations.NextGenManagerAuth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import retrofit2.http.Body;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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

  @POST
  @Timed
  @ExceptionMetered
  @NextGenManagerAuth
  @Path("/sources")
  @ApiOperation(value = "register a list of kubernetes event sources", nickname = "registerKubernetesSources")
  public RestResponse<List<String>> registerKubernetesSources(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @Body @NotNull List<KubernetesActivitySourceDTO> activitySourceDTOs) {
    return new RestResponse<>(kubernetesActivitySourceService.saveKubernetesSources(
        accountId, orgIdentifier, projectIdentifier, activitySourceDTOs));
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
