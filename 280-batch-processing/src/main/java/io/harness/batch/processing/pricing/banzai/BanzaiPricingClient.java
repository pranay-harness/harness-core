/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.pricing.banzai;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface BanzaiPricingClient {
  @GET("status") Call<String> checkServiceStatus();

  @GET("api/v1/providers/{providers}/services/{services}/regions/{regions}/products")
  Call<PricingResponse> getPricingInfo(
      @Path("providers") String providers, @Path("services") String services, @Path("regions") String regions);
}
