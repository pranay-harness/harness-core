package io.harness.secretmanagerclient.services;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.remote.client.RestClientExecutor;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AllArgsConstructor;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class SecretManagerClientServiceImpl implements SecretManagerClientService {
  private final RestClientExecutor restClientExecutor;
  private final SecretManagerClient secretManagerClient;

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(NGAccess ngAccess, DecryptableEntity consumer) {
    BaseNGAccess baseNGAccess = BaseNGAccess.builder()
                                    .accountIdentifier(ngAccess.getAccountIdentifier())
                                    .orgIdentifier(ngAccess.getOrgIdentifier())
                                    .projectIdentifier(ngAccess.getProjectIdentifier())
                                    .identifier(ngAccess.getIdentifier())
                                    .build();
    return restClientExecutor.getResponse(secretManagerClient.getEncryptionDetails(
        NGAccessWithEncryptionConsumer.builder().ngAccess(baseNGAccess).decryptableEntity(consumer).build()));
  }

  @Override
  public SecretManagerConfigDTO getSecretManager(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, boolean maskSecrets) {
    return getResponse(secretManagerClient.getSecretManager(
        identifier, accountIdentifier, orgIdentifier, projectIdentifier, maskSecrets));
  }
}
