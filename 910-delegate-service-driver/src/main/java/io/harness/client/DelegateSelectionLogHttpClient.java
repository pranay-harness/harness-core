/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.client;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.rest.RestResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PIPELINE)
public interface DelegateSelectionLogHttpClient {
  String BASE_API = "ng/delegateSelectionLog/";

  @GET(BASE_API + "delegateInfo")
  Call<RestResponse<DelegateSelectionLogParams>> getDelegateInfo(
      @Query("accountId") String accountId, @Query("taskId") String taskId);
}
