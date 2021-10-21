package io.harness.ng.core.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.beans.DecryptableEntity;
import io.harness.ng.core.DecryptableEntityWithEncryptionConsumers;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.serializer.kryo.KryoRequest;
import io.harness.serializer.kryo.KryoResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface NGSecretDecryptionClient {
  String SECRETS_API = "ng/secrets";

  @POST(SECRETS_API + "/decrypt-encryption-details")
  @KryoRequest
  @KryoResponse
  Call<ResponseDTO<DecryptableEntity>> decryptUsingManager(
      @Body DecryptableEntityWithEncryptionConsumers decryptableEntityWithEncryptionConsumers,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier);
}
