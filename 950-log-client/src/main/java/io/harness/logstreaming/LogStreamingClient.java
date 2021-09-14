/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
