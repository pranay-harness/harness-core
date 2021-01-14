package io.harness.cvng.activity.source.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.ACTIVITY_SOURCE_RESOURCE;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api(ACTIVITY_SOURCE_RESOURCE)
@Path(ACTIVITY_SOURCE_RESOURCE)
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class ActivitySourceResource {
  @Inject private ActivitySourceService activitySourceService;

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "register a kubernetes event source", nickname = "registerActivitySource")
  public RestResponse<String> registerActivitySource(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @Body ActivitySourceDTO activitySourceDTO) {
    return new RestResponse<>(
        activitySourceService.saveActivitySource(accountId, orgIdentifier, projectIdentifier, activitySourceDTO));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets a kubernetes event source by identifier", nickname = "getActivitySource")
  public RestResponse<ActivitySourceDTO> getActivitySource(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("identifier") @NotNull String identifier) {
    return new RestResponse<>(
        activitySourceService.getActivitySource(accountId, orgIdentifier, projectIdentifier, identifier));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/list")
  @ApiOperation(value = "lists all kubernetes event sources", nickname = "listActivitySources")
  public RestResponse<PageResponse<ActivitySourceDTO>> listActivitySources(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @QueryParam("offset") @NotNull Integer offset,
      @QueryParam("pageSize") @NotNull Integer pageSize, @QueryParam("filter") String filter) {
    return new RestResponse<>(activitySourceService.listActivitySources(
        accountId, orgIdentifier, projectIdentifier, offset, pageSize, filter));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @Path("{identifier}")
  @ApiOperation(value = "deletes a kubernetes event source", nickname = "deleteKubernetesSource")
  public RestResponse<Boolean> deleteKubernetesSource(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @PathParam("identifier") @NotNull String identifier) {
    return new RestResponse<>(
        activitySourceService.deleteActivitySource(accountId, orgIdentifier, projectIdentifier, identifier));
  }
}
