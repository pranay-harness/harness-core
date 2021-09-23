package io.harness.feature.services.impl;

import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.beans.FeatureUsageDTO;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FeatureUsageClient {
  String FEATURE_USAGE_API = "/feature-usage";

  @GET(FEATURE_USAGE_API + "/{featureName}")
  Call<ResponseDTO<FeatureUsageDTO>> getFeatureUsage(
      @Path("featureName") FeatureRestriction featureName, @Query("accountIdentifier") String accountIdentifier);

  @POST(FEATURE_USAGE_API + "/custom/{featureName}")
  Call<ResponseDTO<FeatureDetailsDTO>> getCustomFeatureRestrictionDetail(
      @Path("featureName") FeatureRestriction featureName, @Query("accountIdentifier") String accountIdentifier,
      @Body LicensesWithSummaryDTO licensesWithSummaryDTO);
}
