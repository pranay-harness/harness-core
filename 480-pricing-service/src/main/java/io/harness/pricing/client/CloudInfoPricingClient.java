package io.harness.pricing.client;

import io.harness.pricing.dto.cloudinfo.ProductDetailsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CloudInfoPricingClient {
  @GET("status") Call<String> checkServiceStatus();

  @GET("api/v1/providers/{providers}/services/{services}/regions/{regions}/products")
  Call<ProductDetailsResponse> getPricingInfo(
      @Path("providers") String providers, @Path("services") String services, @Path("regions") String regions);
}
