package io.harness.secretmanagerclient.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.ConnectorValidationResult;
import io.harness.ng.core.DecryptableEntityWithEncryptionConsumers;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.dto.EncryptedDataMigrationDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestDTO;
import io.harness.serializer.kryo.KryoRequest;
import io.harness.serializer.kryo.KryoResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface SecretManagerClient {
  String SECRETS_API = "ng/secrets";
  String SECRETS_API_2 = "v2/secrets";
  String SECRET_MANAGERS_API = "ng/secret-managers";

  /*
 GET EncryptedData -> this is to be used for migration purpose
 In case of secret files -> value field will be populated after downloading from file service
  */
  @GET(SECRETS_API + "/migration/{identifier}")
  Call<RestResponse<EncryptedDataMigrationDTO>> getEncryptedDataMigrationDTO(
      @Path(value = "identifier") String identifier,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @Query("decrypted") Boolean decrypted);

  // create secret manager
  @POST(SECRET_MANAGERS_API)
  @KryoRequest
  Call<RestResponse<SecretManagerConfigDTO>> createSecretManager(@Body SecretManagerConfigDTO secretManagerConfig);

  // validate secret manager
  @GET(SECRET_MANAGERS_API + "/{identifier}/validate")
  Call<RestResponse<ConnectorValidationResult>> validateSecretManager(@Path(value = "identifier") String identifier,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  // update secret manager
  @PUT(SECRET_MANAGERS_API + "/{identifier}")
  @KryoRequest
  Call<RestResponse<SecretManagerConfigDTO>> updateSecretManager(@Path("identifier") String identifier,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Body SecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO);

  // list secret managers
  @GET(SECRET_MANAGERS_API)
  Call<RestResponse<List<SecretManagerConfigDTO>>> listSecretManagers(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  // list secret managers
  @GET(SECRET_MANAGERS_API)
  Call<RestResponse<List<SecretManagerConfigDTO>>> listSecretManagers(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(value = NGResourceFilterConstants.IDENTIFIERS) List<String> identifiers);

  // get secret manager
  @GET(SECRET_MANAGERS_API + "/{identifier}")
  Call<RestResponse<SecretManagerConfigDTO>> getSecretManager(@Path("identifier") String identifier,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGCommonEntityConstants.MASK_SECRETS) Boolean maskSecrets);

  // get global secret manager
  @GET(SECRET_MANAGERS_API + "/global/{accountIdentifier}")
  Call<RestResponse<SecretManagerConfigDTO>> getGlobalSecretManager(
      @Path(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier);

  // delete secret manager
  @DELETE(SECRET_MANAGERS_API + "/{identifier}")
  Call<RestResponse<Boolean>> deleteSecretManager(@Path("identifier") String identifier,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  @POST(SECRET_MANAGERS_API + "/meta-data")
  Call<RestResponse<SecretManagerMetadataDTO>> getSecretManagerMetadata(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Body SecretManagerMetadataRequestDTO requestDTO);

  @POST(SECRETS_API_2 + "/decrypt-encryption-details")
  @KryoRequest
  @KryoResponse
  Call<ResponseDTO<DecryptableEntity>> decryptEncryptedDetails(
      @Body DecryptableEntityWithEncryptionConsumers decryptableEntityWithEncryptionConsumers,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier);
}
