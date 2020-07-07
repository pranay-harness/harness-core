package io.harness.ng.core.remote.client.rest;

import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptedDataDetail;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SecretManagerConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

public interface SecretManagerClient {
  String ACCOUNT_ID_KEY = "accountId";
  String SECRET_ID_KEY = "secretId";
  String USER_ID_KEY = "userId";

  String SECRETS_API = "/api/ng/secrets";
  String SECRET_MANAGERS_API = "/api/ng/secret-managers";

  @GET(SECRETS_API + "/{secretId}")
  Call<RestResponse<EncryptedData>> getSecretById(@Path(value = SECRET_ID_KEY) String secretId,
      @Query(value = ACCOUNT_ID_KEY) String accountId, @Query(value = USER_ID_KEY) String userId);

  @GET(SECRETS_API)
  Call<RestResponse<PageResponse<EncryptedData>>> getSecretsForAccountByType(@Query("accountId") String accountId,
      @Query("type") SettingVariableTypes type, @Query("details") boolean includeDetails);

  @POST(SECRETS_API + "/encryption-details")
  Call<RestResponse<List<EncryptedDataDetail>>> getEncryptionDetails(@Query("appId") String appId,
      @Query("workflowExecutionId") String workflowExecutionId, @Body EncryptableSetting encryptableSetting);

  @GET(SECRET_MANAGERS_API)
  Call<RestResponse<List<SecretManagerConfig>>> getSecretManagersForAccount(
      @Query(value = ACCOUNT_ID_KEY) String accountId);

  @GET(SECRET_MANAGERS_API + "/{kmsId}")
  Call<RestResponse<SecretManagerConfig>> getSecretManager(
      @Path("kmsId") String kmsId, @Query("accountId") String accountId);

  @POST(SECRET_MANAGERS_API)
  Call<RestResponse<String>> createOrUpdateSecretManager(
      @Query("accountId") String accountId, @Body SecretManagerConfig secretManagerConfig);

  @DELETE(SECRET_MANAGERS_API + "/{kmsId}")
  Call<RestResponse<Boolean>> deleteSecretManager(@Path("kmsId") String kmsId, @Query("accountId") String accountId);
}