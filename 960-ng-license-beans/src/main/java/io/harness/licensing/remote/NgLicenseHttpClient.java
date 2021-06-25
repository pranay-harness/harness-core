package io.harness.licensing.remote;

import io.harness.licensing.beans.response.CheckExpiryResultDTO;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface NgLicenseHttpClient {
  String CHECK_NG_LICENSE_EXPIRY_API = "licenses/{accountId}/check-expiry";
  String SOFT_DELETE_API = "licenses/{accountId}/soft-delete";

  @GET(CHECK_NG_LICENSE_EXPIRY_API)
  Call<ResponseDTO<CheckExpiryResultDTO>> checkExpiry(@Path("accountId") String accountId);

  @GET(SOFT_DELETE_API) Call<ResponseDTO<Boolean>> softDelete(@Path("accountId") String accountId);
}
