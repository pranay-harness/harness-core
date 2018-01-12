package software.wings.logging;

import com.fasterxml.jackson.databind.JsonNode;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import software.wings.logging.RestLogAppender.LogLines;

/**
 * Created by anubhaw on 1/9/18.
 */
public interface LogdnaRestClient {
  @Headers("Accept: application/json")
  @POST("logs/ingest?now=:now")
  Call<JsonNode> postLogs(
      @Header("Authorization") String basicAuthHeader, @Query("hostname") String hostname, @Body LogLines logLines);
}
