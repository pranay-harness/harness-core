package io.harness.delegate.task.citasks.awsvm.helper;

import io.harness.delegate.beans.ci.awsvm.runner.ExecuteStepResponse;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface RunnerRestClient {
  @POST("setup") @Headers("Accept: application/json") Call<Void> setup(@Body Map<String, String> parameters);

  @POST("step")
  @Headers("Accept: application/json")
  Call<ExecuteStepResponse> step(@Body Map<String, String> parameters);

  @POST("destroy") @Headers("Accept: application/json") Call<Void> destroy(@Body Map<String, String> parameters);
}
