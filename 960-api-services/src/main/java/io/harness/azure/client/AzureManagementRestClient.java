package io.harness.azure.client;

import io.harness.azure.model.management.ManagementGroupListResult;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface AzureManagementRestClient {
  @GET("providers/Microsoft.Management/managementGroups?api-version=2020-02-01")
  Call<ManagementGroupListResult> listManagementGroups(@Header("Authorization") String bearerAuthHeader);
}
