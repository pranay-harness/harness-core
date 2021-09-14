/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.azure.client;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

public interface AzureManagementRestClient {
  String APP_VERSION = "2020-10-01";

  @GET("providers/Microsoft.Management/managementGroups?api-version=2020-10-01")
  Observable<Response<ResponseBody>> listManagementGroups(@Header("Authorization") String bearerAuthHeader);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @GET
  Observable<Response<ResponseBody>> listNext(
      @Header("Authorization") String bearerAuthHeader, @Url String nextUrl, @Query("api-version") String appVersion);
}
