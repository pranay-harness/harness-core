package io.harness.ff;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

@OwnedBy(HarnessTeam.PL)
public interface FeatureFlagsClient {
  String FEATURE_FLAGS_API = "feature-flag";

  @GET(FEATURE_FLAGS_API + "/{featureName}")
  Call<RestResponse<FeatureFlagDTO>> getFeatureFlagName(@Path("featureName") String featureFlagName);
}
