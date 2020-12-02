package io.harness.logstreaming;

import io.harness.delegate.beans.logstreaming.LogLine;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface DelegateAgentLogStreamingClient {
  @POST("stream")
  Call<Void> openLogStream(
      @Header("X-Harness-Token") String authToken, @Query("accountID") String accountId, @Query("key") String logKey);

  @DELETE("stream")
  Call<Void> closeLogStream(@Header("X-Harness-Token") String authToken, @Query("accountID") String accountId,
      @Query("key") String logKey, @Query("snapshot") boolean snapshot);

  @PUT("stream")
  Call<Void> pushMessage(@Header("X-Harness-Token") String authToken, @Query("accountID") String accountId,
      @Query("key") String logKey, @Body List<LogLine> messages);
}
