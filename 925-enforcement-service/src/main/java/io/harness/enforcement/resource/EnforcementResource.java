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

@Api("/enforcement")
@Path("/enforcement")
@Produces({"application/json"})
@Consumes({"application/json"})
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
  @NGAccessControlCheck(resourceType = RESOURCE_TYPE, permission = PERMISSION)
  public ResponseDTO<FeatureRestrictionDetailsDTO> getFeatureRestrictionDetail(
      @NotNull @Valid @Body FeatureRestrictionDetailRequestDTO requestDTO,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.getFeatureDetail(requestDTO.getName(), accountIdentifier));
  }

  @GET
  @Path("/enabled")
  @ApiOperation(value = "Gets List of Enabled Feature Restriction Detail for The Account",
      nickname = "getEnabledFeatureRestrictionDetailByAccountId")
  @NGAccessControlCheck(resourceType = RESOURCE_TYPE, permission = PERMISSION)
  public ResponseDTO<List<FeatureRestrictionDetailsDTO>>
  getEnabledFeatureRestrictionForAccount(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.getEnabledFeatureDetails(accountIdentifier));
  }

  @GET
  @Path("/metadata")
  @ApiOperation(value = "Gets All Feature Restriction Metadata", nickname = "getAllFeatureRestrictionMetadata")
  public ResponseDTO<List<FeatureRestrictionMetadataDTO>> getAllFeatureRestrictionMetadata() {
    return ResponseDTO.newResponse(featureService.getAllFeatureRestrictionMetadata());
  }

  @GET
  @Path("/{featureRestrictionName}/metadata")
  @ApiOperation(value = "Get Feature Restriction Metadata", nickname = "getFeatureRestrictionMetadata", hidden = true)
  @InternalApi
  public ResponseDTO<FeatureRestrictionMetadataDTO> getFeatureRestrictionMetadata(
      @NotNull @PathParam(FEATURE_RESTRICTION_NAME) FeatureRestrictionName featureRestrictionName,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.getFeatureMetadata(featureRestrictionName, accountIdentifier));
  }

  @POST
  @Path("/metadata")
  @ApiOperation(value = "Get Map of Feature Restriction and its Metadata",
      nickname = "getFeatureRestrictionMetadataMap", hidden = true)
  @InternalApi
  public ResponseDTO<RestrictionMetadataMapResponseDTO>
  getFeatureRestrictionMetadataMap(@NotNull @Body RestrictionMetadataMapRequestDTO restrictionMetadataMapRequestDTO,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.getFeatureRestrictionMetadataMap(
        restrictionMetadataMapRequestDTO.getNames(), accountIdentifier));
  }
}
