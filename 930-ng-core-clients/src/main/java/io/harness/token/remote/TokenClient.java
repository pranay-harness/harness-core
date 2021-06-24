package io.harness.token.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.TokenDTO;

import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TokenClient {
  @GET("token")
  Call<ResponseDTO<TokenDTO>> getToken(@Query(NGCommonEntityConstants.TOKEN_KEY) @NotEmpty String tokenId);
}
