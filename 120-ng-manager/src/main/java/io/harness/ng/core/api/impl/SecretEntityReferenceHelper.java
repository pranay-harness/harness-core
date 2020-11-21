package io.harness.ng.core.api.impl;

import static io.harness.NGConstants.ENTITY_REFERENCE_LOG_PREFIX;

import io.harness.EntityType;
import io.harness.common.EntityReference;
import io.harness.entitysetupusageclient.EntitySetupUsageHelper;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.IdentifierRefHelper;
import io.harness.utils.RestCallToNGManagerClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class SecretEntityReferenceHelper {
  EntitySetupUsageHelper entityReferenceHelper;
  EntitySetupUsageClient entitySetupUsageClient;

  public void createEntityReferenceForSecret(EncryptedDataDTO encryptedDataDTO) {
    String secretMangerFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(encryptedDataDTO.getAccount(),
        encryptedDataDTO.getOrg(), encryptedDataDTO.getProject(), encryptedDataDTO.getSecretManager());
    String secretFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(encryptedDataDTO.getAccount(),
        encryptedDataDTO.getOrg(), encryptedDataDTO.getProject(), encryptedDataDTO.getIdentifier());
    EntityReference secretReference =
        IdentifierRefHelper.getIdentifierRefFromEntityIdentifiers(encryptedDataDTO.getIdentifier(),
            encryptedDataDTO.getAccount(), encryptedDataDTO.getOrg(), encryptedDataDTO.getProject());

    EntityReference secretManagerReference =
        IdentifierRefHelper.getIdentifierRefFromEntityIdentifiers(encryptedDataDTO.getSecretManager(),
            encryptedDataDTO.getAccount(), encryptedDataDTO.getOrg(), encryptedDataDTO.getProject());

    EntityDetail secretDetails = EntityDetail.builder()
                                     .entityRef(secretReference)
                                     .type(EntityType.SECRETS)
                                     .name(encryptedDataDTO.getName())
                                     .build();
    EntityDetail secretManagerDetails = EntityDetail.builder()
                                            .entityRef(secretManagerReference)
                                            .type(EntityType.CONNECTORS)
                                            .name(encryptedDataDTO.getSecretManagerName())
                                            .build();
    EntitySetupUsageDTO entityReferenceDTO =
        entityReferenceHelper.createEntityReference(encryptedDataDTO.getAccount(), secretManagerDetails, secretDetails);
    try {
      RestCallToNGManagerClientUtils.execute(entitySetupUsageClient.save(entityReferenceDTO));
    } catch (Exception ex) {
      log.info(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not created when the secret [{}] was created from the secret manager [{}]",
          secretFQN, secretMangerFQN);
    }
  }

  public void deleteSecretEntityReferenceWhenSecretGetsDeleted(EncryptedDataDTO encryptedDataDTO) {
    String secretMangerFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(encryptedDataDTO.getAccount(),
        encryptedDataDTO.getOrg(), encryptedDataDTO.getProject(), encryptedDataDTO.getSecretManager());
    String secretFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(encryptedDataDTO.getAccount(),
        encryptedDataDTO.getOrg(), encryptedDataDTO.getProject(), encryptedDataDTO.getIdentifier());
    boolean entityReferenceDeleted = false;
    try {
      entityReferenceDeleted = RestCallToNGManagerClientUtils.execute(
          entitySetupUsageClient.deleteAllReferredByEntityRecords(encryptedDataDTO.getAccount(), secretFQN));
    } catch (Exception ex) {
      log.info(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not deleted when the secret [{}] was deleted from the secret manager [{}] with the exception [{}]",
          secretFQN, secretMangerFQN, ex.getMessage());
    }
    if (entityReferenceDeleted) {
      log.info(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not deleted when the secret [{}] was deleted from the secret manager [{}]",
          secretFQN, secretMangerFQN);
    }
  }
}
