/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.remote;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestDTO;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;

@OwnedBy(PL)
@Api("secret-managers")
@Path("secret-managers")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Tag(name = "SecretManagers", description = "This contains APIs related to SecretManagers as defined in Harness")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class NGSecretManagerResource {
  @Inject private final NGSecretManagerService ngSecretManagerService;

  @POST
  @Path("meta-data")
  @ApiOperation(value = "Get metadata of secret manager", nickname = "getMetadata")
  @Operation(operationId = "getMetadata", summary = "Gets the metadata of Secret Manager",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the metadata of Secret Manager")
      })
  public ResponseDTO<SecretManagerMetadataDTO>
  getSecretEngines(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
                       NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @RequestBody(required = true, description = "Details required for the creation of the Secret Manager") @NotNull
      @Valid SecretManagerMetadataRequestDTO requestDTO) {
    return ResponseDTO.newResponse(ngSecretManagerService.getMetadata(accountIdentifier, requestDTO));
  }

  @GET
  @Hidden
  @Path("{identifier}")
  @ApiOperation(hidden = true, value = "Get Secret Manager", nickname = "getSecretManager")
  @Operation(operationId = "getSecretManager", summary = "Gets the Secret Manager Config",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns Secret Manager Config")
      })
  @InternalApi
  public ResponseDTO<SecretManagerConfigDTO>
  getSecretManager(@Parameter(description = "Secret Manager Identifier") @NotNull @PathParam(
                       NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Parameter(
          description = "Specify whether or not to mask the secrets. If left empty default value of true is assumed")
      @QueryParam(NGCommonEntityConstants.MASK_SECRETS) @DefaultValue("true") Boolean maskSecrets) {
    return ResponseDTO.newResponse(ngSecretManagerService.getSecretManager(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, Boolean.TRUE.equals(maskSecrets)));
  }
}
