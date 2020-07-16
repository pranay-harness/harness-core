package io.harness.secretmanagerclient.remote;

import io.harness.beans.PageResponse;
import io.harness.encryption.SecretType;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.kryo.KryoRequest;
import io.harness.serializer.kryo.KryoResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SecretManagerConfig;

import java.util.List;

public interface SecretManagerClient {
  String ACCOUNT_ID_KEY = "accountId";
  String SECRET_ID_KEY = "secretId";
  String USER_ID_KEY = "userId";

  String SECRETS_API = "/api/ng/secrets";
  String SECRET_MANAGERS_API = "/api/ng/secret-managers";

  @GET(SECRETS_API + "/{secretId}")
  Call<RestResponse<EncryptedDataDTO>> getSecretById(@Path(value = SECRET_ID_KEY) String secretId,
      @Query(value = ACCOUNT_ID_KEY) String accountId, @Query(value = USER_ID_KEY) String userId);

  @POST(SECRETS_API)
  Call<RestResponse<String>> createSecret(@Query(value = ACCOUNT_ID_KEY) String accountId,
      @Query(value = "local") boolean localMode, @Body SecretTextDTO secretText);

  @PUT(SECRETS_API)
  Call<RestResponse<Boolean>> updateSecret(@Query(value = ACCOUNT_ID_KEY) String accountId,
      @Query(value = "uuid") String uuId, @Body SecretTextDTO secretText);

  @DELETE(SECRETS_API)
  Call<RestResponse<Boolean>> deleteSecret(
      @Query(value = ACCOUNT_ID_KEY) String accountId, @Query(value = "uuid") String uuId);

  @GET(SECRETS_API)
  Call<RestResponse<PageResponse<EncryptedDataDTO>>> getSecretsForAccountByType(
      @Query("accountId") String accountId, @Query("type") SecretType secretType);

  @GET(SECRET_MANAGERS_API)
  @KryoResponse
  Call<RestResponse<List<SecretManagerConfig>>> getSecretManagersForAccount(
      @Query(value = ACCOUNT_ID_KEY) String accountId);

  @GET(SECRET_MANAGERS_API + "/{kmsId}")
  @KryoResponse
  Call<RestResponse<SecretManagerConfig>> getSecretManager(
      @Path("kmsId") String kmsId, @Query("accountId") String accountId);

  @POST(SECRET_MANAGERS_API)
  @KryoRequest
  Call<RestResponse<String>> createOrUpdateSecretManager(
      @Query("accountId") String accountId, @Body SecretManagerConfig secretManagerConfig);

  @DELETE(SECRET_MANAGERS_API + "/{kmsId}")
  Call<RestResponse<Boolean>> deleteSecretManager(@Path("kmsId") String kmsId, @Query("accountId") String accountId);

  @POST(SecretManagerClient.SECRETS_API + "/encryption-details")
  @KryoRequest
  @KryoResponse
  Call<RestResponse<List<EncryptedDataDetail>>> getEncryptionDetails(@Body EncryptableSetting encryptableSetting);
}
