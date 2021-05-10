package io.harness.cenextgenclient;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.GET;

@OwnedBy(CE)
public interface CENextGenResourceClient {
  String BASE_API = "ccm/api";

  @GET(BASE_API + "/") Call<ResponseDTO<Boolean>> test();
}
