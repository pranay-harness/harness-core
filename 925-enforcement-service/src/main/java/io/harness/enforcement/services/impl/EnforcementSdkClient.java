/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
  @PUT("enforcement/client/usage/{featureRestrictionName}")
  Call<ResponseDTO<FeatureRestrictionUsageDTO>> getRestrictionUsage(
      @Path("featureRestrictionName") FeatureRestrictionName featureRestrictionName,
      @Query("accountIdentifier") String accountIdentifier, @Body RestrictionMetadataDTO restrictionMetadataDTO);

  @PUT("enforcement/client/custom/{featureRestrictionName}")
  Call<ResponseDTO<Boolean>> evaluateCustomFeatureRestriction(
      @Path("featureRestrictionName") FeatureRestrictionName featureRestrictionName,
      @Query("accountIdentifier") String accountIdentifier,
      @Body CustomRestrictionEvaluationDTO customFeatureEvaluationDTO);
}
