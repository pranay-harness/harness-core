package io.harness.secrets.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SecretNGManagerClient {
  String SECRETS_API = "v2/secrets";

  @GET(SECRETS_API + "/{identifier}")
  Call<ResponseDTO<SecretResponseWrapper>> getSecret(
      @Path(value = NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  @POST(SECRETS_API + "/encryption-details")
  Call<RestResponse<List<EncryptedDataDetail>>> getEncryptionDetails(
      @Body NGAccessWithEncryptionConsumer ngAccessWithEncryptionConsumer);
}
