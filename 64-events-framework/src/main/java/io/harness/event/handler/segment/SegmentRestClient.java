package io.harness.event.handler.segment;

import io.harness.event.model.segment.Identity;
import io.harness.event.model.segment.Response;
import io.harness.event.model.segment.Trace;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface SegmentRestClient {
  @Headers("Accept: application/json")
  @POST("identify")
  Call<Response> identity(@Header(value = "Authorization") String apiKey, @Body Identity identity);

  @Headers("Accept: application/json")
  @POST("track")
  Call<Response> track(@Header(value = "Authorization") String apiKey, @Body Trace trace);
}
