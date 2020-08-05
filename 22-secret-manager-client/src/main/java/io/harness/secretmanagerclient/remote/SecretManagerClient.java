package io.harness.secretmanagerclient.remote;

import static io.harness.secretmanagerclient.NGConstants.ACCOUNT_IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.DESCRIPTION_KEY;
import static io.harness.secretmanagerclient.NGConstants.FILE_KEY;
import static io.harness.secretmanagerclient.NGConstants.IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.NAME_KEY;
import static io.harness.secretmanagerclient.NGConstants.ORG_IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.PROJECT_IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.SECRET_MANAGER_IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.TAGS_KEY;

import io.harness.beans.PageResponse;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretTextCreateDTO;
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

import java.util.List;

public interface SecretManagerClient {
  String SECRETS_API = "/api/ng/secrets";
  String SECRET_FILES_API = "/api/ng/secret-files";
  String SECRET_MANAGERS_API = "/api/ng/secret-managers";

  // create secret
  @POST(SECRETS_API) Call<RestResponse<EncryptedDataDTO>> createSecret(@Body SecretTextCreateDTO secretText);

  // create secret file
  @Multipart
  @POST(SECRET_FILES_API)
  Call<RestResponse<EncryptedDataDTO>> createSecretFile(@Part(NAME_KEY) RequestBody name,
      @Part(TAGS_KEY) RequestBody tags, @Part(DESCRIPTION_KEY) RequestBody description,
      @Part(ACCOUNT_IDENTIFIER_KEY) RequestBody accountIdentifier, @Part(ORG_IDENTIFIER_KEY) RequestBody orgIdentifier,
      @Part(PROJECT_IDENTIFIER_KEY) RequestBody projectIdentifier, @Part(IDENTIFIER_KEY) RequestBody identifier,
      @Part(SECRET_MANAGER_IDENTIFIER_KEY) RequestBody secretManagerIdentifier, @Part(FILE_KEY) RequestBody file);

  // update secret file
  @Multipart
  @PUT(SECRET_FILES_API + "/{identifier}")
  Call<RestResponse<Boolean>> updateSecretFile(@Path(IDENTIFIER_KEY) String identifier,
      @Part(TAGS_KEY) RequestBody tags, @Part(DESCRIPTION_KEY) RequestBody description,
      @Part(ACCOUNT_IDENTIFIER_KEY) RequestBody accountIdentifier, @Part(ORG_IDENTIFIER_KEY) RequestBody orgIdentifier,
      @Part(PROJECT_IDENTIFIER_KEY) RequestBody projectIdentifier, @Part(FILE_KEY) RequestBody file);

  // get secret
  @GET(SECRETS_API + "/{identifier}")
  Call<RestResponse<EncryptedDataDTO>> getSecret(@Path(value = "identifier") String identifier,
      @Query(value = ACCOUNT_IDENTIFIER_KEY) String accountIdentifier, @Query(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @Query(PROJECT_IDENTIFIER_KEY) String projectIdentifier);

  // list secrets
  @GET(SECRETS_API)
  Call<RestResponse<PageResponse<EncryptedDataDTO>>> listSecrets(
      @Query(value = ACCOUNT_IDENTIFIER_KEY) String accountIdentifier, @Query(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @Query(PROJECT_IDENTIFIER_KEY) String projectIdentifier, @Query("type") SecretType secretType,
      @Query("searchTerm") String searchTerm);

  // update secret
  @PUT(SECRETS_API + "/{identifier}")
  Call<RestResponse<Boolean>> updateSecret(@Path("identifier") String identifier,
      @Query(value = ACCOUNT_IDENTIFIER_KEY) String accountIdentifier, @Query(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @Query(PROJECT_IDENTIFIER_KEY) String projectIdentifier, @Body SecretTextUpdateDTO secretText);

  // delete secret
  @DELETE(SECRETS_API + "/{identifier}")
  Call<RestResponse<Boolean>> deleteSecret(@Path(value = "identifier") String identifier,
      @Query(value = ACCOUNT_IDENTIFIER_KEY) String accountIdentifier, @Query(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @Query(PROJECT_IDENTIFIER_KEY) String projectIdentifier);

  // get encryption details
  @POST(SecretManagerClient.SECRETS_API + "/encryption-details")
  @KryoRequest
  @KryoResponse
  Call<RestResponse<List<EncryptedDataDetail>>> getEncryptionDetails(
      @Body NGAccessWithEncryptionConsumer ngAccessWithEncryptionConsumer);

  // create secret manager
  @POST(SECRET_MANAGERS_API)
  Call<RestResponse<SecretManagerConfigDTO>> createSecretManager(@Body SecretManagerConfigDTO secretManagerConfig);

  // update secret manager
  @PUT(SECRET_MANAGERS_API + "/{identifier}")
  Call<RestResponse<SecretManagerConfigDTO>> updateSecretManager(@Path("identifier") String identifier,
      @Query(ACCOUNT_IDENTIFIER_KEY) String accountIdentifier, @Query(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @Query(PROJECT_IDENTIFIER_KEY) String projectIdentifier,
      @Body NGSecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO);

  // list secret managers
  @GET(SECRET_MANAGERS_API)
  Call<RestResponse<List<SecretManagerConfigDTO>>> listSecretManagers(
      @Query(value = ACCOUNT_IDENTIFIER_KEY) String accountIdentifier,
      @Query(value = ORG_IDENTIFIER_KEY) String orgIdentifier,
      @Query(value = PROJECT_IDENTIFIER_KEY) String projectIdentifier);

  // get secret manager
  @GET(SECRET_MANAGERS_API + "/{identifier}")
  Call<RestResponse<SecretManagerConfigDTO>> getSecretManager(@Path("identifier") String identifier,
      @Query(ACCOUNT_IDENTIFIER_KEY) String accountIdentifier, @Query(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @Query(PROJECT_IDENTIFIER_KEY) String projectIdentifier);

  // delete secret manager
  @DELETE(SECRET_MANAGERS_API + "/{identifier}")
  Call<RestResponse<Boolean>> deleteSecretManager(@Path("identifier") String identifier,
      @Query(ACCOUNT_IDENTIFIER_KEY) String accountIdentifier, @Query(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @Query(PROJECT_IDENTIFIER_KEY) String projectIdentifier);
}
