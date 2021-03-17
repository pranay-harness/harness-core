package io.harness.accesscontrol.clients;

import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AccessControlHttpClient {
  String ACL_API = "/api/acl";

  @POST(ACL_API)
  Call<ResponseDTO<HAccessCheckResponseDTO>> getAccessControlList(@Body HAccessCheckRequestDTO accessCheckRequestDTO);
}
