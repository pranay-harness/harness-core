/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.approval.resources;

import io.harness.NGCommonEntityConstants;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.approval.ApprovalResourceService;
import io.harness.steps.approval.step.beans.ApprovalInstanceResponseDTO;
import io.harness.steps.approval.step.beans.ApprovalType;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivityRequestDTO;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalInstanceAuthorizationDTO;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("approvals")
@Path("approvals")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = NGCommonEntityConstants.BAD_REQUEST_CODE,
    description = NGCommonEntityConstants.BAD_REQUEST_PARAM_MESSAGE,
    content =
    {
      @Content(mediaType = NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE,
          schema = @Schema(implementation = FailureDTO.class))
      ,
          @Content(mediaType = NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE,
              schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = NGCommonEntityConstants.INTERNAL_SERVER_ERROR_CODE,
    description = NGCommonEntityConstants.INTERNAL_SERVER_ERROR_MESSAGE,
    content =
    {
      @Content(mediaType = NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE,
          schema = @Schema(implementation = ErrorDTO.class))
      ,
          @Content(mediaType = NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE,
              schema = @Schema(implementation = ErrorDTO.class))
    })
@PipelineServiceAuth
@Slf4j
public class ApprovalResource {
  private final ApprovalResourceService approvalResourceService;

  public static final String APPROVAL_PARAM_MESSAGE = "Approval Identifier for the entity";

  @Inject
  public ApprovalResource(ApprovalResourceService approvalResourceService) {
    this.approvalResourceService = approvalResourceService;
  }

  @GET
  @Path("/{approvalInstanceId}")
  @ApiOperation(value = "Gets an Approval Instance by identifier", nickname = "getApprovalInstance")
  @Operation(operationId = "getApprovalInstance", summary = "Gets an Approval Instance by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the saved Approval Instance")
      })
  @Hidden
  public ResponseDTO<ApprovalInstanceResponseDTO>
  getApprovalInstance(@Parameter(description = APPROVAL_PARAM_MESSAGE) @NotEmpty @PathParam(
      "approvalInstanceId") String approvalInstanceId) {
    return ResponseDTO.newResponse(approvalResourceService.get(approvalInstanceId));
  }

  @POST
  @Path("/{approvalInstanceId}/harness/activity")
  @ApiOperation(value = "Add a new Harness Approval activity", nickname = "addHarnessApprovalActivity")
  @Operation(operationId = "addHarnessApprovalActivity", summary = "Add a new Harness Approval activity",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns a newly added Harness Approval activity")
      })
  @Hidden
  public ResponseDTO<ApprovalInstanceResponseDTO>
  addHarnessApprovalActivity(@Parameter(description = APPROVAL_PARAM_MESSAGE) @NotEmpty @PathParam(
                                 "approvalInstanceId") String approvalInstanceId,
      @Parameter(description = "This contains the details of Harness Approval Activity requested") @NotNull
      @Valid HarnessApprovalActivityRequestDTO request) {
    return ResponseDTO.newResponse(approvalResourceService.addHarnessApprovalActivity(approvalInstanceId, request));
  }

  @GET
  @Path("/{approvalInstanceId}/harness/authorization")
  @ApiOperation(value = "Gets a Harness Approval Instance authorization for the current user",
      nickname = "getHarnessApprovalInstanceAuthorization")
  @Operation(operationId = "getHarnessApprovalInstanceAuthorization",
      summary = "Gets a Harness Approval Instance authorization for the current user",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "Returns the Harness Approval instance authorization for the current user")
      })
  @Hidden
  public ResponseDTO<HarnessApprovalInstanceAuthorizationDTO>
  getHarnessApprovalInstanceAuthorization(@Parameter(description = APPROVAL_PARAM_MESSAGE) @NotEmpty @PathParam(
      "approvalInstanceId") String approvalInstanceId) {
    return ResponseDTO.newResponse(approvalResourceService.getHarnessApprovalInstanceAuthorization(approvalInstanceId));
  }

  @GET
  @Path("/stage-yaml-snippet")
  @ApiOperation(value = "Gets the initial yaml snippet for Approval stage", nickname = "getInitialStageYamlSnippet")
  @Hidden
  public ResponseDTO<String> getInitialStageYamlSnippet(@Parameter(description = "Approval Type") @NotNull @QueryParam(
      "approvalType") ApprovalType approvalType) throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    return ResponseDTO.newResponse(
        Resources.toString(Objects.requireNonNull(classLoader.getResource(
                               String.format("approval_stage_yamls/%s.yaml", approvalType.getDisplayName()))),
            StandardCharsets.UTF_8));
  }
}
