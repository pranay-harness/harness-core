package io.harness.managerclient;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.rest.RestResponse;
import io.harness.serializer.kryo.KryoResponse;

import software.wings.beans.ConfigFile;
import software.wings.beans.Delegate;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ManagerClient {
  @GET("service-templates/{templateId}/compute-files")
  Call<RestResponse<List<ConfigFile>>> getConfigFiles(@Path("templateId") String templateId,
      @Query("accountId") String accountId, @Query("appId") String appId, @Query("envId") String envId,
      @Query("hostId") String hostId);

  @KryoResponse
  @POST("agent/delegates/{delegateId}/tasks/{taskId}/report")
  Call<DelegateTaskPackage> reportConnectionResults(@Path("delegateId") String delegateId, @Path("taskId") String uuid,
      @Query("accountId") String accountId, @Body List<DelegateConnectionResult> results);

  @POST("agent/delegates/heartbeat-with-polling")
  Call<RestResponse<Delegate>> delegateHeartbeat(@Query("accountId") String accountId, @Body Delegate delegate);
}
