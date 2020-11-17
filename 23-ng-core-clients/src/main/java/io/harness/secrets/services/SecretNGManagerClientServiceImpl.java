package io.harness.secrets.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.beans.DecryptableEntity;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AllArgsConstructor;
import software.wings.annotation.EncryptableSetting;

import java.util.List;

import static io.harness.remote.client.RestClientUtils.getResponse;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class SecretNGManagerClientServiceImpl implements SecretManagerClientService {
  private final SecretNGManagerClient secretManagerClient;

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting encryptableSetting) {
    throw new UnsupportedOperationException("This method no longer supported.");
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(NGAccess ngAccess, DecryptableEntity consumer) {
    BaseNGAccess baseNGAccess = BaseNGAccess.builder()
                                    .accountIdentifier(ngAccess.getAccountIdentifier())
                                    .orgIdentifier(ngAccess.getOrgIdentifier())
                                    .projectIdentifier(ngAccess.getProjectIdentifier())
                                    .identifier(ngAccess.getIdentifier())
                                    .build();
    return getResponse(secretManagerClient.getEncryptionDetails(
        NGAccessWithEncryptionConsumer.builder().ngAccess(baseNGAccess).decryptableEntity(consumer).build()));
  }
}
