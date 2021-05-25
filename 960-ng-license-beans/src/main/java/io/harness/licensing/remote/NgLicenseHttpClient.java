package io.harness.licensing.remote;

import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface NgLicenseHttpClient {
  String CHECK_NG_LICENSE_INACTIVE_API = "licenses/{accountId}/inactive-status";
  String SOFT_DELETE_API = "licenses/{accountId}/soft-delete";

  @GET(CHECK_NG_LICENSE_INACTIVE_API)
  Call<ResponseDTO<Boolean>> checkNGLicensesAllInactive(@Path("accountId") String accountId);

  @GET(SOFT_DELETE_API) Call<ResponseDTO<Boolean>> softDelete(@Path("accountId") String accountId);
}
