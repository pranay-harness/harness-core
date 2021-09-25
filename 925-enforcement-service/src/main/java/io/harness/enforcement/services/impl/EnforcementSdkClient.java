package io.harness.enforcement.services.impl;

import io.harness.enforcement.beans.FeatureRestrictionUsageDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface EnforcementSdkClient {
  @GET("/enforcement/client/usage/{featureRestrictionName}")
  Call<ResponseDTO<FeatureRestrictionUsageDTO>> getRestrictionUsage(
      @Path("featureRestrictionName") FeatureRestrictionName featureRestrictionName,
      @Query("accountIdentifier") String accountIdentifier);
}
