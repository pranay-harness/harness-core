package io.harness.licensing.remote.admin;

import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AdminLicenseHttpClient {
  String ADMIN_LICENSE_API = "admin/licenses";

  @GET(ADMIN_LICENSE_API + "/{accountIdentifier}")
  Call<ResponseDTO<AccountLicenseDTO>> getAccountLicense(@Path("accountIdentifier") String accountIdentifier);

  @POST(ADMIN_LICENSE_API)
  Call<ResponseDTO<AccountLicenseDTO>> createAccountLicense(
      @Query("accountIdentifier") String accountIdentifier, @Body AccountLicenseDTO accountLicenseDTO);

  @PUT(ADMIN_LICENSE_API + "/{identifier}")
  Call<ResponseDTO<AccountLicenseDTO>> updateAccountLicense(@Path("identifier") String identifier,
      @Query("accountIdentifier") String accountIdentifier, @Body AccountLicenseDTO accountLicenseDTO);
}
