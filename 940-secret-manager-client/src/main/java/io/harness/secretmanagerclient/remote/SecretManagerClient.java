package io.harness.secretmanagerclient.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.dto.EncryptedDataMigrationDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestDTO;
import io.harness.serializer.kryo.KryoRequest;

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
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  // delete secret
  @DELETE(SECRETS_API + "/{identifier}")
  Call<RestResponse<Boolean>> deleteSecret(@Path(value = "identifier") String identifier,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

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
}
