package io.harness.cvng.activity.source.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.KUBERNETES_RESOURCE;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.activity.beans.KubernetesActivityDetailsDTO;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.activity.source.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.change.KubernetesChangeEventDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api(KUBERNETES_RESOURCE)
@Path(KUBERNETES_RESOURCE)
@Produces("application/json")
@ExposeInternalException
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class KubernetesActivityResource {
  @Inject private KubernetesActivitySourceService kubernetesActivitySourceService;
  @Inject private ActivitySourceService activitySourceService;

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

  @POST
  @Timed
  @ExceptionMetered
  @DelegateAuth
  @Path("/change")
  @ApiOperation(value = "saves a  kubernetes change", nickname = "saveKubernetesChange")
  public RestResponse<Boolean> saveKubernetesChange(@QueryParam("accountId") @NotNull String accountId,
      @NotNull @Valid @Body KubernetesChangeEventDTO changeEventDTO) {
    return new RestResponse<>(kubernetesActivitySourceService.saveKubernetesChange(accountId, changeEventDTO));
  }

  @GET
  @Timed
  @ExceptionMetered
  @NextGenManagerAuth
  @Path("/namespaces")
  @ApiOperation(value = "gets a list of kubernetes namespaces", nickname = "getNamespaces")
  public ResponseDTO<PageResponse<String>> getNamespaces(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("connectorIdentifier") @NotNull String connectorIdentifier,
      @QueryParam("offset") @NotNull Integer offset, @QueryParam("pageSize") @NotNull Integer pageSize,
      @QueryParam("filter") String filter) {
    return ResponseDTO.newResponse(kubernetesActivitySourceService.getKubernetesNamespaces(
        accountId, orgIdentifier, projectIdentifier, connectorIdentifier, offset, pageSize, filter));
  }

  @GET
  @Timed
  @ExceptionMetered
  @NextGenManagerAuth
  @Path("/workloads")
  @ApiOperation(value = "gets a list of kubernetes workloads", nickname = "getWorkloads")
  public ResponseDTO<PageResponse<String>> getWorkloads(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("connectorIdentifier") @NotNull String connectorIdentifier,
      @QueryParam("namespace") @NotNull String namespace, @QueryParam("offset") @NotNull Integer offset,
      @QueryParam("pageSize") @NotNull Integer pageSize, @QueryParam("filter") String filter) {
    return ResponseDTO.newResponse(kubernetesActivitySourceService.getKubernetesWorkloads(
        accountId, orgIdentifier, projectIdentifier, connectorIdentifier, namespace, offset, pageSize, filter));
  }

  @GET
  @Path("/source")
  @Timed
  @ExceptionMetered
  @DelegateAuth
  @ApiOperation(value = "gets kubernetes source for data collection", nickname = "getKubernetesSource")
  public RestResponse<KubernetesActivitySourceDTO> getKubernetesSource(
      @QueryParam("accountId") @NotNull String accountId,
      @QueryParam("dataCollectionWorkerId") @NotNull String dataCollectionWorkerId) {
    return new RestResponse<>(
        (KubernetesActivitySourceDTO) activitySourceService.getActivitySource(dataCollectionWorkerId).toDTO());
  }

  @GET
  @Timed
  @ExceptionMetered
  @NextGenManagerAuth
  @Path("/event-details")
  @ApiOperation(value = "gets details of kubernetes events", nickname = "getEventDetails")
  public ResponseDTO<KubernetesActivityDetailsDTO> getEventDetails(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("activityId") @NotNull String activityId) {
    return ResponseDTO.newResponse(
        kubernetesActivitySourceService.getEventDetails(accountId, orgIdentifier, projectIdentifier, activityId));
  }
}
