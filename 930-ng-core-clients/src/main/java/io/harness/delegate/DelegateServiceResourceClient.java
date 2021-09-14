/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
