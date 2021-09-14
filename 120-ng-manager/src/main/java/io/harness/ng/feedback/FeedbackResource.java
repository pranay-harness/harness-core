/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.feedback;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.feedback.beans.FeedbackFormDTO;
import io.harness.ng.feedback.services.FeedbackService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@OwnedBy(HarnessTeam.GTM)
@Api("/feedback")
@Path("/feedback")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class FeedbackResource {
  private final FeedbackService feedbackService;

  @Inject
  public FeedbackResource(FeedbackService feedbackService) {
    this.feedbackService = feedbackService;
  }

  @POST
  @ApiOperation(value = "Saves Feedback", nickname = "saveFeedback")
  public ResponseDTO<Boolean> saveFeedback(
      @QueryParam("accountIdentifier") String accountIdentifier, FeedbackFormDTO dto) {
    return ResponseDTO.newResponse(feedbackService.saveFeedback(dto));
  }
}
