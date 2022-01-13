/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.azure.client;

import software.wings.helpers.ext.azure.AcrGetRepositoryTagsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;

public interface AzureContainerRegistryRestClient {
  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @GET("/v2/{repositoryName}/tags/list?n=500&orderby=timedesc")
  Call<AcrGetRepositoryTagsResponse> listRepositoryTags(@Header("Authorization") String basicAuthHeader,
      @Path(value = "repositoryName", encoded = true) String repositoryName);
}
