package io.harness.ng.core.api.impl;

import static io.harness.ng.NGConstants.ENTITY_REFERENCE_LOG_PREFIX;
import static io.harness.ng.core.RestCallToNGManagerClientUtils.execute;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.entityreferenceclient.remote.EntityReferenceClient;
import io.harness.ng.EntityType;
import io.harness.ng.core.entityReference.EntityReferenceHelper;
import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class SecretEntityReferenceHelper {
  EntityReferenceHelper entityReferenceHelper;
  EntityReferenceClient entityReferenceClient;

  public void createEntityReferenceForSecret(EncryptedDataDTO encryptedDataDTO) {
    String secretMangerFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(encryptedDataDTO.getAccount(),
        encryptedDataDTO.getOrg(), encryptedDataDTO.getProject(), encryptedDataDTO.getSecretManager());
    String secretFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(encryptedDataDTO.getAccount(),
        encryptedDataDTO.getOrg(), encryptedDataDTO.getProject(), encryptedDataDTO.getIdentifier());
    EntityReferenceDTO entityReferenceDTO = entityReferenceHelper.createEntityReference(encryptedDataDTO.getAccount(),
        encryptedDataDTO.getSecretManagerName(), EntityType.CONNECTORS, secretMangerFQN, secretFQN,
        encryptedDataDTO.getName(), EntityType.SECRETS);
    try {
      execute(entityReferenceClient.save(entityReferenceDTO));
    } catch (Exception ex) {
      logger.info(ENTITY_REFERENCE_LOG_PREFIX
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
      entityReferenceDeleted = execute(entityReferenceClient.delete(secretMangerFQN, secretFQN));
    } catch (Exception ex) {
      logger.info(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not deleted when the secret [{}] was deleted from the secret manager [{}] with the exception [{}]",
          secretFQN, secretMangerFQN, ex.getMessage());
    }
    if (entityReferenceDeleted) {
      logger.info(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not deleted when the secret [{}] was deleted from the secret manager [{}]",
          secretFQN, secretMangerFQN);
    }
  }
}
