package io.harness.secretmanagerclient.remote;

import static io.harness.ng.NGConstants.ACCOUNT_KEY;
import static io.harness.ng.NGConstants.FILE_KEY;
import static io.harness.ng.NGConstants.FILE_METADATA_KEY;
import static io.harness.ng.NGConstants.IDENTIFIER_KEY;
import static io.harness.ng.NGConstants.ORG_KEY;
import static io.harness.ng.NGConstants.PAGE_KEY;
import static io.harness.ng.NGConstants.PROJECT_KEY;
import static io.harness.ng.NGConstants.SIZE_KEY;

import io.harness.beans.PageResponse;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.kryo.KryoRequest;
import io.harness.serializer.kryo.KryoResponse;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.settings.SettingVariableTypes;

import java.util.List;

public interface SecretManagerClient {
  String SECRETS_API = "ng/secrets";
  String SECRET_FILES_API = "ng/secret-files";
  String SECRET_MANAGERS_API = "ng/secret-managers";

  // create secret
  @KryoRequest
  @KryoResponse
  @POST(SECRETS_API)
  Call<RestResponse<EncryptedDataDTO>> createSecret(@Body SecretTextDTO secretText);

  // create secret file
  @Multipart
  @POST(SECRET_FILES_API)
  Call<RestResponse<EncryptedDataDTO>> createSecretFile(
      @Part(FILE_METADATA_KEY) RequestBody metaData, @Part(FILE_KEY) RequestBody file);

  // update secret file
  @Multipart
  @PUT(SECRET_FILES_API + "/{identifier}")
  Call<RestResponse<Boolean>> updateSecretFile(@Path(IDENTIFIER_KEY) String identifier,
      @Query(ACCOUNT_KEY) String accountIdentifier, @Query(ORG_KEY) String orgIdentifier,
      @Query(PROJECT_KEY) String projectIdentifier, @Part(FILE_KEY) RequestBody file,
      @Part(FILE_METADATA_KEY) RequestBody metaData);

  // get secret
  @GET(SECRETS_API + "/{identifier}")
  Call<RestResponse<EncryptedDataDTO>> getSecret(@Path(value = "identifier") String identifier,
      @Query(value = ACCOUNT_KEY) String accountIdentifier, @Query(ORG_KEY) String orgIdentifier,
      @Query(PROJECT_KEY) String projectIdentifier);

  // list secrets
  @GET(SECRETS_API)
  Call<RestResponse<PageResponse<EncryptedDataDTO>>> listSecrets(@Query(value = ACCOUNT_KEY) String accountIdentifier,
      @Query(ORG_KEY) String orgIdentifier, @Query(PROJECT_KEY) String projectIdentifier,
      @Query("type") SettingVariableTypes secretType, @Query("searchTerm") String searchTerm, @Query(PAGE_KEY) int page,
      @Query(SIZE_KEY) int size);

  // update secret
  @PUT(SECRETS_API + "/{identifier}")
  @KryoRequest
  Call<RestResponse<Boolean>> updateSecret(@Path("identifier") String identifier,
      @Query(value = ACCOUNT_KEY) String accountIdentifier, @Query(ORG_KEY) String orgIdentifier,
      @Query(PROJECT_KEY) String projectIdentifier, @Body SecretTextUpdateDTO secretText);

  // delete secret
  @DELETE(SECRETS_API + "/{identifier}")
  Call<RestResponse<Boolean>> deleteSecret(@Path(value = "identifier") String identifier,
      @Query(value = ACCOUNT_KEY) String accountIdentifier, @Query(ORG_KEY) String orgIdentifier,
      @Query(PROJECT_KEY) String projectIdentifier);

  // get encryption details
  @POST(SecretManagerClient.SECRETS_API + "/encryption-details")
  @KryoRequest
  @KryoResponse
  Call<RestResponse<List<EncryptedDataDetail>>> getEncryptionDetails(
      @Body NGAccessWithEncryptionConsumer ngAccessWithEncryptionConsumer);

  // create secret manager
  @POST(SECRET_MANAGERS_API)
  @KryoRequest
  @KryoResponse
  Call<RestResponse<SecretManagerConfigDTO>> createSecretManager(@Body SecretManagerConfigDTO secretManagerConfig);

  // update secret manager
  @PUT(SECRET_MANAGERS_API + "/{identifier}")
  @KryoResponse
  @KryoRequest
  Call<RestResponse<SecretManagerConfigDTO>> updateSecretManager(@Path("identifier") String identifier,
      @Query(ACCOUNT_KEY) String accountIdentifier, @Query(ORG_KEY) String orgIdentifier,
      @Query(PROJECT_KEY) String projectIdentifier, @Body SecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO);

  // list secret managers
  @GET(SECRET_MANAGERS_API)
  Call<RestResponse<List<SecretManagerConfigDTO>>> listSecretManagers(
      @Query(value = ACCOUNT_KEY) String accountIdentifier, @Query(value = ORG_KEY) String orgIdentifier,
      @Query(value = PROJECT_KEY) String projectIdentifier);

  // get secret manager
  @GET(SECRET_MANAGERS_API + "/{identifier}")
  Call<RestResponse<SecretManagerConfigDTO>> getSecretManager(@Path("identifier") String identifier,
      @Query(ACCOUNT_KEY) String accountIdentifier, @Query(ORG_KEY) String orgIdentifier,
      @Query(PROJECT_KEY) String projectIdentifier);

  // get global secret manager
  @GET(SECRET_MANAGERS_API + "/global/{accountIdentifier}")
  Call<RestResponse<SecretManagerConfigDTO>> getGlobalSecretManager(@Path(ACCOUNT_KEY) String accountIdentifier);

  // delete secret manager
  @DELETE(SECRET_MANAGERS_API + "/{identifier}")
  Call<RestResponse<Boolean>> deleteSecretManager(@Path("identifier") String identifier,
      @Query(ACCOUNT_KEY) String accountIdentifier, @Query(ORG_KEY) String orgIdentifier,
      @Query(PROJECT_KEY) String projectIdentifier);
}
