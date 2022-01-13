/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
