package io.harness.cenextgenclient;

import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.GET;

public interface CENextGenResourceClient {
  String BASE_API = "ceng/api";

  @GET(BASE_API + "/") Call<ResponseDTO<Boolean>> test();
}
