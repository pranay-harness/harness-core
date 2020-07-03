package io.harness.ng.core.remote.client.rest;

import io.harness.rest.RestResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.security.encryption.EncryptedData;

public interface SecretManagerClient {
  String ACCOUNT_ID_KEY = "accountId";
  String SECRET_ID_KEY = "secretId";
  String USER_ID_KEY = "userId";

  String GET_SECRET_BY_ID_API = "/api/ng/secrets/{secretId}";

  @GET(GET_SECRET_BY_ID_API)
  Call<RestResponse<EncryptedData>> getSecretById(@Path(value = SECRET_ID_KEY) String secretId,
      @Query(value = ACCOUNT_ID_KEY) String accountId, @Query(value = USER_ID_KEY) String userId);
}