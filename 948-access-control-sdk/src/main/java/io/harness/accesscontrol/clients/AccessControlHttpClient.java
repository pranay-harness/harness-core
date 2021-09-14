/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

@OwnedBy(HarnessTeam.PL)
public interface AccessControlHttpClient {
  String ACL_API = "acl";

  @POST(ACL_API)
  Call<ResponseDTO<AccessCheckResponseDTO>> checkForAccess(@Body AccessCheckRequestDTO accessCheckRequestDTO);
}
