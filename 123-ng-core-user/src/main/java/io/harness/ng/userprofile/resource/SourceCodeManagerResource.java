/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.userprofile.resource;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_CODE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_CODE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.userprofile.commons.SourceCodeManagerDTO;
import io.harness.ng.userprofile.services.api.SourceCodeManagerService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@OwnedBy(PL)
@Api("source-code-manager")
@Path("source-code-manager")
@Produces("application/json")
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = BAD_REQUEST_PARAM_MESSAGE)
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = INTERNAL_SERVER_ERROR_MESSAGE)
    })
@Tag(name = "Source Code Manager", description = "Contains APIs related to Source Code Manager")
@io.swagger.v3.oas.annotations.responses.
ApiResponse(responseCode = BAD_REQUEST_CODE, description = BAD_REQUEST_PARAM_MESSAGE,
    content =
    {
      @Content(mediaType = APPLICATION_JSON_MEDIA_TYPE, schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = APPLICATION_YAML_MEDIA_TYPE, schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.
ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = INTERNAL_SERVER_ERROR_MESSAGE,
    content =
    {
      @Content(mediaType = APPLICATION_JSON_MEDIA_TYPE, schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = APPLICATION_YAML_MEDIA_TYPE, schema = @Schema(implementation = ErrorDTO.class))
    })

@NextGenManagerAuth
public class SourceCodeManagerResource {
  @Inject SourceCodeManagerService sourceCodeManagerService;

  @GET
  @ApiOperation(value = "get source code manager information", nickname = "getSourceCodeManagers")
  @Operation(operationId = "getSourceCodeManagers", summary = "Lists Source Code Managers for the given account",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Paginated list of Source Code Managers of given account")
      })
  public ResponseDTO<List<SourceCodeManagerDTO>>
  get(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
      "accountIdentifier") String accountIdentifier) {
    return ResponseDTO.newResponse(sourceCodeManagerService.get(accountIdentifier));
  }

  @POST
  @ApiOperation(value = "save source code manager", nickname = "saveSourceCodeManagers")
  @Operation(operationId = "createSourceCodeManager", summary = "Creates Source Code Manager",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "This contains details of the newly created Source Code Manager")
      })
  public ResponseDTO<SourceCodeManagerDTO>
  save(@RequestBody(description = "This contains details of Source Code Manager") @NotNull @Body
      @Valid SourceCodeManagerDTO sourceCodeManagerDTO) {
    return ResponseDTO.newResponse(sourceCodeManagerService.save(sourceCodeManagerDTO));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "update source code manager", nickname = "updateSourceCodeManagers")
  @Operation(operationId = "updateSourceCodeManager",
      summary = "Updates Source Code Manager Details with the given Source Code Manager Id",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            description =
                "This contains details of the updated Source Code Manager for the specific Source Code Manager Id")
      })
  public ResponseDTO<SourceCodeManagerDTO>
  update(@Parameter(description = "Source Code manager Identifier") @NotNull @PathParam(
             "identifier") String sourceCodeManagerIdentifier,
      @RequestBody(description = "This contains details of Source Code Manager") @NotNull @Body
      @Valid SourceCodeManagerDTO sourceCodeManagerDTO) {
    return ResponseDTO.newResponse(sourceCodeManagerService.update(sourceCodeManagerIdentifier, sourceCodeManagerDTO));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "delete source code manager", nickname = "deleteSourceCodeManagers")
  @Operation(operationId = "deleteSourceCodeManager",
      summary = "Deletes the Source Code Manager corresponding to the specified Source Code Manager Id",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Successfully deleted Source Code Manager for the give Source Code Manager Id")
      })
  public ResponseDTO<Boolean>
  delete(@Parameter(description = "Source Code manager Identifier") @NotNull @PathParam(
             "identifier") String sourceCodeManagerName,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          "accountIdentifier") String accountIdentifier) {
    return ResponseDTO.newResponse(sourceCodeManagerService.delete(sourceCodeManagerName, accountIdentifier));
  }
}
