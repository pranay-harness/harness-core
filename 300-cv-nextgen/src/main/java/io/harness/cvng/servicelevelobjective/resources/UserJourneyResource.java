package io.harness.cvng.servicelevelobjective.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.UserJourneyDTO;
import io.harness.cvng.servicelevelobjective.beans.UserJourneyResponse;
import io.harness.cvng.servicelevelobjective.services.UserJourneyService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("user-journey")
@Path("user-journey")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
@OwnedBy(HarnessTeam.CV)
public class UserJourneyResource {
  @Inject UserJourneyService userJourneyService;

  @POST
  @Timed
  @ExceptionMetered
  @Path("/create")
  @ApiOperation(value = "saves user journey", nickname = "saveUserJourney")
  public RestResponse<UserJourneyResponse> saveUserJourney(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @NotNull @Valid @Body UserJourneyDTO userJourneyDTO) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return new RestResponse<>(userJourneyService.create(projectParams, userJourneyDTO));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all user journeys", nickname = "getAllJourneys")
  public ResponseDTO<PageResponse<UserJourneyResponse>> getAllJourneys(
      @NotNull @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @QueryParam("offset") @NotNull Integer offset,
      @QueryParam("pageSize") @NotNull Integer pageSize) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return ResponseDTO.newResponse(userJourneyService.getUserJourneys(projectParams, offset, pageSize));
  }
}
