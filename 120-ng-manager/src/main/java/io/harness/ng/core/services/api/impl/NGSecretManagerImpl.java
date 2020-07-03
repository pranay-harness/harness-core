package io.harness.ng.core.services.api.impl;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ng.core.remote.client.rest.SecretManagerClient;
import io.harness.rest.RestResponse;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.impl.security.SecretManagementException;

import java.io.IOException;

/**
 * At present I am deliberately extending from <code> NoOpSecretManagerImpl </code> as
 * majority of method are not required.  As we start adding more implementation
 * will make this implement <code> SecretManager </code>
 */
@Singleton
@Slf4j
public class NGSecretManagerImpl extends NoOpSecretManagerImpl {
  private final SecretManagerClient secretManagerClient;

  @Inject
  public NGSecretManagerImpl(SecretManagerClient secretManagerClient) {
    this.secretManagerClient = secretManagerClient;
  }

  @Override
  public EncryptedData getSecretById(String accountId, String id) {
    EncryptedData encryptedData = null;
    RestResponse<EncryptedData> encryptedDataRestResponse = null;
    try {
      Response<RestResponse<EncryptedData>> response =
          secretManagerClient.getSecretById(id, accountId, "USER_ID").execute();
      if (response != null && response.isSuccessful()) {
        encryptedDataRestResponse = response.body();
        if (encryptedDataRestResponse != null) {
          encryptedData = encryptedDataRestResponse.getResource();
        }
      }
    } catch (IOException ex) {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "IOException exception occurred while fetching secret", ex, USER);
    }

    if (encryptedData == null) {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, String.format("Secrete with id %s & account %s not found", id, accountId), USER);
    }
    return encryptedData;
  }
}
