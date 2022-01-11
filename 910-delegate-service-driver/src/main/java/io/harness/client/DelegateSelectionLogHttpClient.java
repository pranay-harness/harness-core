/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
