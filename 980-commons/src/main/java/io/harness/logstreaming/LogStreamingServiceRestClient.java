/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.logstreaming;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.DEL)
public interface LogStreamingServiceRestClient {
  @GET("token")
  Call<String> retrieveAccountToken(
      @Header("X-Harness-Token") String serviceToken, @Query("accountID") String accountId);
}
