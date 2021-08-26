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
