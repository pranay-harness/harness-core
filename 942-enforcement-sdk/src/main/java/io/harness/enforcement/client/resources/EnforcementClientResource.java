/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.client.resources;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.enforcement.beans.CustomRestrictionEvaluationDTO;
import io.harness.enforcement.beans.FeatureRestrictionUsageDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.client.custom.CustomRestrictionInterface;
import io.harness.enforcement.client.services.EnforcementSdkRegisterService;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api(value = "/enforcement/client", hidden = true)
@Path("/enforcement/client")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class EnforcementClientResource {
  private static final String FEATURE_RESTRICTION_NAME = "featureRestrictionName";
  @Inject private EnforcementSdkRegisterService enforcementSdkRegisterService;

  @PUT
  @Path("usage/{featureRestrictionName}")
  @InternalApi
  public ResponseDTO<FeatureRestrictionUsageDTO> getFeatureUsage(
      @NotNull @PathParam(FEATURE_RESTRICTION_NAME) FeatureRestrictionName featureRestrictionName,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @Valid @Body RestrictionMetadataDTO restrictionMetadataDTO) {
    RestrictionUsageInterface restrictionUsageInterface =
        enforcementSdkRegisterService.getRestrictionUsageInterface(featureRestrictionName);
    if (restrictionUsageInterface == null) {
      return ResponseDTO.newResponse(FeatureRestrictionUsageDTO.builder().count(0).build());
    }
    return ResponseDTO.newResponse(
        FeatureRestrictionUsageDTO.builder()
            .count(restrictionUsageInterface.getCurrentValue(accountIdentifier, restrictionMetadataDTO))
            .build());
  }

  @PUT
  @Path("custom/{featureRestrictionName}")
  @InternalApi
  public ResponseDTO<Boolean> evaluateCustomFeatureRestriction(
      @NotNull @PathParam(FEATURE_RESTRICTION_NAME) FeatureRestrictionName featureRestrictionName,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @Valid @Body CustomRestrictionEvaluationDTO customFeatureEvaluationDTO) {
    CustomRestrictionInterface customRestrictionInterface =
        enforcementSdkRegisterService.getCustomRestrictionInterface(featureRestrictionName);
    if (customRestrictionInterface == null) {
      throw new InvalidRequestException(String.format(
          "FeatureRestriction [%s] is not registered with CustomRestrictionInterface", featureRestrictionName));
    }
    return ResponseDTO.newResponse(customRestrictionInterface.evaluateCustomRestriction(customFeatureEvaluationDTO));
  }
}
