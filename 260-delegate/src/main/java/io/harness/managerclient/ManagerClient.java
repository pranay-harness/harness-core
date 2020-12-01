package io.harness.managerclient;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.serializer.kryo.KryoResponse;

import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ManagerClient {
  @KryoResponse
  @POST("agent/delegates/{delegateId}/tasks/{taskId}/report")
  Call<DelegateTaskPackage> reportConnectionResults(@Path("delegateId") String delegateId, @Path("taskId") String uuid,
      @Query("accountId") String accountId, @Body List<DelegateConnectionResult> results);
}
