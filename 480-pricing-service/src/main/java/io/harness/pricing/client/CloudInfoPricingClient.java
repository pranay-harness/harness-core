/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pricing.client;

import io.harness.pricing.dto.cloudinfo.ProductDetailResponse;
import io.harness.pricing.dto.cloudinfo.ProductDetailsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CloudInfoPricingClient {
  @GET("status") Call<String> checkServiceStatus();

  @GET("api/v1/providers/{providers}/services/{services}/regions/{regions}/products")
  Call<ProductDetailsResponse> getPricingInfo(
      @Path("providers") String providers, @Path("services") String services, @Path("regions") String regions);

  @GET("api/v1/providers/{providers}/services/{services}/regions/{regions}/product/{product}")
  Call<ProductDetailResponse> getPricingInfo(@Path("providers") String providers, @Path("services") String services,
      @Path("regions") String regions, @Path("product") String product);
}
