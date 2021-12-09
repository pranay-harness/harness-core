package io.harness.delegate.task.citasks.vm.helper;

import io.harness.delegate.beans.ci.vm.runner.DestroyVmRequest;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepRequest;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepResponse;
import io.harness.delegate.beans.ci.vm.runner.SetupVmRequest;
import io.harness.delegate.beans.ci.vm.runner.SetupVmResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface RunnerRestClient {
  @POST("setup") @Headers("Accept: application/json") Call<SetupVmResponse> setup(@Body SetupVmRequest setupVmRequest);

  @POST("step")
  @Headers("Accept: application/json")
  Call<ExecuteStepResponse> step(@Body ExecuteStepRequest executeStepRequest);

  @POST("destroy") @Headers("Accept: application/json") Call<Void> destroy(@Body DestroyVmRequest destroyVmRequest);
}
