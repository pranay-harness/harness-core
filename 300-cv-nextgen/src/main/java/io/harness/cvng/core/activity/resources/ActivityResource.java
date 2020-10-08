package io.harness.cvng.core.activity.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.ACTIVITY_RESOURCE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.beans.ActivityDTO;
import io.harness.cvng.beans.KubernetesActivityDTO;
import io.harness.cvng.core.activity.beans.KubernetesActivitySourceDTO;
import io.harness.cvng.core.activity.services.api.KubernetesActivitySourceService;
import io.harness.cvng.core.beans.DeploymentActivityResultDTO;
import io.harness.cvng.core.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.core.services.api.ActivityService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.harness.security.annotations.PublicApi;
import io.swagger.annotations.Api;
import retrofit2.http.Body;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(ACTIVITY_RESOURCE)
@Path(ACTIVITY_RESOURCE)
@Produces("application/json")
@PublicApi
public class ActivityResource {
  @Inject private ActivityService activityService;
  @Inject private KubernetesActivitySourceService kubernetesActivitySourceService;

  @POST
  @Timed
  @ExceptionMetered
  @Path("{webHookToken}")
  public void registerKubernetesSource(@PathParam("webHookToken") String webHookToken,
      @QueryParam("accountId") @Valid final String accountId, @Body ActivityDTO activityDTO) {
    activityService.register(accountId, webHookToken, activityDTO);
  }

  @GET
  @Path("recent-deployment-activity-verifications")
  public RestResponse<List<DeploymentActivityVerificationResultDTO>> getRecentDeploymentActivityVerifications(
      @QueryParam("accountId") String accountId, @QueryParam("projectIdentifier") String projectIdentifier) {
    return new RestResponse<>(activityService.getRecentDeploymentActivityVerifications(accountId, projectIdentifier));
  }

  @GET
  @Path("deployment-activity-verifications/{deploymentTag}")
  public RestResponse<DeploymentActivityResultDTO> getDeploymentActivityVerificationsByTag(
      @QueryParam("accountId") String accountId, @QueryParam("projectIdentifier") String projectIdentifier,
      @PathParam("deploymentTag") String deploymentTag) {
    return new RestResponse(
        activityService.getDeploymentActivityVerificationsByTag(accountId, projectIdentifier, deploymentTag));
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/kubernetes-source")
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
  @DelegateAuth
  @Path("/kubernetes-activities")
  public RestResponse<Boolean> saveKubernetesActivities(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("activitySourceId") @NotNull String activitySourceId,
      @NotNull @Valid @Body List<KubernetesActivityDTO> activities) {
    return new RestResponse<>(
        kubernetesActivitySourceService.saveKubernetesActivities(accountId, activitySourceId, activities));
  }
}
