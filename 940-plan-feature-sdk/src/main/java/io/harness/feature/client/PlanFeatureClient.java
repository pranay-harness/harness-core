package io.harness.feature.client;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.GTM)
public interface PlanFeatureClient {
  String FEATURE_PLAN_API = "/plan-feature";

  @GET(FEATURE_PLAN_API + "/{featureName}/check")
  Call<ResponseDTO<Boolean>> checkFeatureAvailability(@Path("featureName") FeatureRestriction featureName,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier, @Query("currentUsage") long currentUsage);
}
