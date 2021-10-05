package io.harness.enforcement.services.impl;

import io.harness.enforcement.beans.CustomRestrictionEvaluationDTO;
import io.harness.enforcement.beans.FeatureRestrictionUsageDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface EnforcementSdkClient {
  @PUT("/enforcement/client/usage/{featureRestrictionName}")
  Call<ResponseDTO<FeatureRestrictionUsageDTO>> getRestrictionUsage(
      @Path("featureRestrictionName") FeatureRestrictionName featureRestrictionName,
      @Query("accountIdentifier") String accountIdentifier, @Body RestrictionMetadataDTO restrictionMetadataDTO);

  @PUT("/enforcement/client/custom/{featureRestrictionName}")
  Call<ResponseDTO<Boolean>> evaluateCustomFeatureRestriction(
      @Path("featureRestrictionName") FeatureRestrictionName featureRestrictionName,
      @Query("accountIdentifier") String accountIdentifier,
      @Body CustomRestrictionEvaluationDTO customFeatureEvaluationDTO);
}
