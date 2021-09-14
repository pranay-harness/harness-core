/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.remote.admin;

import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
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
  Call<ResponseDTO<ModuleLicenseDTO>> createAccountLicense(
      @Query("accountIdentifier") String accountIdentifier, @Body ModuleLicenseDTO moduleLicenseDTO);

  @PUT(ADMIN_LICENSE_API + "/{identifier}")
  Call<ResponseDTO<ModuleLicenseDTO>> updateModuleLicense(@Path("identifier") String identifier,
      @Query("accountIdentifier") String accountIdentifier, @Body ModuleLicenseDTO moduleLicenseDTO);
}
