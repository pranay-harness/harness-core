package io.harness.enforcement.resource;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailRequestDTO;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.internal.RestrictionMetadataMapRequestDTO;
import io.harness.enforcement.beans.internal.RestrictionMetadataMapResponseDTO;
import io.harness.enforcement.beans.metadata.FeatureRestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.services.EnforcementService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("enforcement")
@Path("enforcement")
@Produces({"application/json"})
@Consumes({"application/json"})
@Tag(name = "Enforcement", description = "This contains APIs related to enforcement as defined in Harness")
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
public class EnforcementResource {
  private static final String FEATURE_RESTRICTION_NAME = "featureRestrictionName";
  private static final String RESOURCE_TYPE = "ACCOUNT";
  private static final String PERMISSION = "core_account_view";
  @Inject EnforcementService featureService;

  @POST
  @ApiOperation(value = "Gets Feature Restriction Detail", nickname = "getFeatureRestrictionDetail")
  @Operation(operationId = "getFeatureRestrictionDetail", summary = "Gets Feature Restriction Detail",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns a feature restriction details DTO")
      })
  @NGAccessControlCheck(resourceType = RESOURCE_TYPE, permission = PERMISSION)
  public ResponseDTO<FeatureRestrictionDetailsDTO>
  getFeatureRestrictionDetail(@NotNull @Valid @Body FeatureRestrictionDetailRequestDTO requestDTO,
      @Parameter(description = "Account id to get the feature restriction detail.") @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.getFeatureDetail(requestDTO.getName(), accountIdentifier));
  }

  @GET
  @Path("/enabled")
  @ApiOperation(value = "Gets List of Enabled Feature Restriction Detail for The Account",
      nickname = "getEnabledFeatureRestrictionDetailByAccountId")
  @Operation(operationId = "getEnabledFeatureRestrictionDetailByAccountId",
      summary = "Gets List of Enabled Feature Restriction Detail for The Account",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns a list of freature restriction details DTO")
      })
  @NGAccessControlCheck(resourceType = RESOURCE_TYPE, permission = PERMISSION)
  public ResponseDTO<List<FeatureRestrictionDetailsDTO>>
  getEnabledFeatureRestrictionForAccount(
      @Parameter(description = "Account id to get the enable features for the account") @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.getEnabledFeatureDetails(accountIdentifier));
  }

  @GET
  @Path("/metadata")
  @ApiOperation(value = "Gets All Feature Restriction Metadata", nickname = "getAllFeatureRestrictionMetadata")
  @Operation(operationId = "getAllFeatureRestrictionMetadata", summary = "Gets All Feature Restriction Metadata",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns a list of feature restriction metadata dto")
      })
  public ResponseDTO<List<FeatureRestrictionMetadataDTO>>
  getAllFeatureRestrictionMetadata() {
    return ResponseDTO.newResponse(featureService.getAllFeatureRestrictionMetadata());
  }

  @GET
  @Path("/{featureRestrictionName}/metadata")
  @ApiOperation(value = "Get Feature Restriction Metadata", nickname = "getFeatureRestrictionMetadata", hidden = true)
  @InternalApi
  public ResponseDTO<FeatureRestrictionMetadataDTO> getFeatureRestrictionMetadata(
      @Parameter(description = "The feature restriction to retrieve metadata from.") @NotNull @PathParam(
          FEATURE_RESTRICTION_NAME) FeatureRestrictionName featureRestrictionName,
      @Parameter(description = "Account id to get the feature metadata.") @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.getFeatureMetadata(featureRestrictionName, accountIdentifier));
  }

  @POST
  @Path("/metadata")
  @ApiOperation(value = "Get Map of Feature Restriction and its Metadata",
      nickname = "getFeatureRestrictionMetadataMap", hidden = true)
  @InternalApi
  public ResponseDTO<RestrictionMetadataMapResponseDTO>
  getFeatureRestrictionMetadataMap(@NotNull @Body RestrictionMetadataMapRequestDTO restrictionMetadataMapRequestDTO,
      @Parameter(description = "Account id to get all metadata from.") @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.getFeatureRestrictionMetadataMap(
        restrictionMetadataMapRequestDTO.getNames(), accountIdentifier));
  }
}
