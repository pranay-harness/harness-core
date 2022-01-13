/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.license.remote;

import io.harness.ccm.license.CeLicenseInfoDTO;
import io.harness.rest.RestResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface CeLicenseClient {
  String CG_LICENSE_API = "ng/licenses/ce";

  @POST(CG_LICENSE_API + "/trial") Call<RestResponse<Boolean>> createCeTrial(@Body CeLicenseInfoDTO ceLicenseInfoDTO);

  @PUT(CG_LICENSE_API) Call<RestResponse<Boolean>> updateCeLicense(@Body CeLicenseInfoDTO ceLicenseInfoDTO);
}
