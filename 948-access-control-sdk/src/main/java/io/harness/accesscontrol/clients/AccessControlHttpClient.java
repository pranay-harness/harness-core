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
