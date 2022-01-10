/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.delegate.DelegateConfigResourceValidationResponse;
import io.harness.ng.core.delegate.DelegateResourceValidationResponse;
import io.harness.rest.RestResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateServiceResourceClient {
  String BASE_API = "ng/delegate-service/resource-validation";

  @GET(BASE_API + "/delegates")
  Call<RestResponse<DelegateResourceValidationResponse>> validateDelegates(@Query("accountId") String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(value = NGResourceFilterConstants.IDENTIFIERS) List<String> identifiers);

  @GET(BASE_API + "/delegate-configs")
  Call<RestResponse<DelegateConfigResourceValidationResponse>> validateDelegateConfigurations(
      @Query("accountId") String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(value = NGResourceFilterConstants.IDENTIFIERS) List<String> identifiers);
}
