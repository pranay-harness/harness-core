/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.logstreaming;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.DEL)
public interface LogStreamingClient {
  @POST("stream")
  Call<Void> openLogStream(
      @Header("X-Harness-Token") String authToken, @Query("accountID") String accountId, @Query("key") String logKey);

  @DELETE("stream")
  Call<Void> closeLogStream(@Header("X-Harness-Token") String authToken, @Query("accountID") String accountId,
      @Query("key") String logKey, @Query("snapshot") boolean snapshot);

  @PUT("stream")
  Call<Void> pushMessage(@Header("X-Harness-Token") String authToken, @Query("accountID") String accountId,
      @Query("key") String logKey, @Body List<LogLine> messages);

  @DELETE("stream")
  Call<Void> closeLogStreamWithPrefix(@Header("X-Harness-Token") String authToken, @Query("accountID") String accountId,
      @Query("key") String logKey, @Query("snapshot") boolean snapshot, @Query("prefix") boolean prefix);
}
