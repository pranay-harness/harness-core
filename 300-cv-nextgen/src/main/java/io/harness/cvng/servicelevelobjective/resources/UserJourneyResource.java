/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.resources;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.UserJourneyDTO;
import io.harness.cvng.servicelevelobjective.beans.UserJourneyResponse;
import io.harness.cvng.servicelevelobjective.services.api.UserJourneyService;
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

  public static final String SLO = "SLO";
  public static final String VIEW_PERMISSION = "chi_slo_view";

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
  @NGAccessControlCheck(resourceType = SLO, permission = VIEW_PERMISSION)
  public ResponseDTO<PageResponse<UserJourneyResponse>> getAllJourneys(
      @NotNull @QueryParam("accountId") @AccountIdentifier String accountId,
      @QueryParam("orgIdentifier") @NotNull @OrgIdentifier String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull @ProjectIdentifier String projectIdentifier,
      @QueryParam("offset") @NotNull Integer offset, @QueryParam("pageSize") @NotNull Integer pageSize) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return ResponseDTO.newResponse(userJourneyService.getUserJourneys(projectParams, offset, pageSize));
  }
}
