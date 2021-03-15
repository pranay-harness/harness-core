package io.harness.delegatelog.client;

import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.rest.RestResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DelegateSelectionLogHttpClient {
  String BASE_API = "ng/delegateSelectionLog/";

  @GET(BASE_API + "delegateInfo")
  Call<RestResponse<DelegateSelectionLogParams>> getDelegateInfo(
      @Query("accountId") String accountId, @Query("taskId") String taskId);
}
