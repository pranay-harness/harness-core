package io.harness.ng.core.api.impl;

import static io.harness.NGConstants.ENTITY_REFERENCE_LOG_PREFIX;
import static io.harness.ng.eventsframework.EventsFrameworkModule.SETUP_USAGE_CREATE;

import static software.wings.utils.Utils.emptyIfNull;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.entitysetupusageclient.EntitySetupUsageHelper;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entitysetupusage.DeleteSetupUsageDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SecretEntityReferenceHelper {
  EntitySetupUsageHelper entityReferenceHelper;
  EntitySetupUsageClient entitySetupUsageClient;
  AbstractProducer eventProducer;

  @Inject
  public SecretEntityReferenceHelper(EntitySetupUsageHelper entityReferenceHelper,
      EntitySetupUsageClient entitySetupUsageClient, @Named(SETUP_USAGE_CREATE) AbstractProducer eventProducer) {
    this.entityReferenceHelper = entityReferenceHelper;
    this.entitySetupUsageClient = entitySetupUsageClient;
    this.eventProducer = eventProducer;
  }

  public void createSetupUsageForSecretManager(EncryptedDataDTO encryptedDataDTO) {
    String secretMangerFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(encryptedDataDTO.getAccount(),
        encryptedDataDTO.getOrg(), encryptedDataDTO.getProject(), encryptedDataDTO.getSecretManager());
    String secretFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(encryptedDataDTO.getAccount(),
        encryptedDataDTO.getOrg(), encryptedDataDTO.getProject(), encryptedDataDTO.getIdentifier());
    IdentifierRefProtoDTO secretReference = createIdentifierReferenceForEvent(encryptedDataDTO.getIdentifier(),
        encryptedDataDTO.getAccount(), encryptedDataDTO.getOrg(), encryptedDataDTO.getProject());

    IdentifierRefProtoDTO secretManagerReference =
        createIdentifierReferenceForEvent(encryptedDataDTO.getSecretManager(), encryptedDataDTO.getAccount(),
            encryptedDataDTO.getOrg(), encryptedDataDTO.getProject());

    EntityDetailProtoDTO secretDetails = EntityDetailProtoDTO.newBuilder()
                                             .setIdentifierRef(secretReference)
                                             .setType(EntityTypeProtoEnum.SECRETS)
                                             .setName(emptyIfNull(encryptedDataDTO.getName()))
                                             .build();

    EntityDetailProtoDTO secretManagerDetails = EntityDetailProtoDTO.newBuilder()
                                                    .setIdentifierRef(secretManagerReference)
                                                    .setType(EntityTypeProtoEnum.CONNECTORS)
                                                    .setName(emptyIfNull(encryptedDataDTO.getSecretManagerName()))
                                                    .build();
    EntitySetupUsageCreateDTO entityReferenceDTO = EntitySetupUsageCreateDTO.newBuilder()
                                                       .setAccountIdentifier(encryptedDataDTO.getAccount())
                                                       .setReferredByEntity(secretDetails)
                                                       .setReferredEntity(secretManagerDetails)
                                                       .build();
    try {
      eventProducer.send(Message.newBuilder()
                             .putMetadata("accountId", encryptedDataDTO.getAccount())
                             .setData(entityReferenceDTO.toByteString())
                             .build());
    } catch (Exception ex) {
      log.info(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not created when the secret [{}] was created from the secret manager [{}]",
          secretFQN, secretMangerFQN);
    }
  }

  private IdentifierRefProtoDTO createIdentifierReferenceForEvent(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    IdentifierRefProtoDTO.Builder identifierRefBuilder = IdentifierRefProtoDTO.newBuilder()
                                                             .setIdentifier(StringValue.of(identifier))
                                                             .setAccountIdentifier(StringValue.of(accountIdentifier));
    if (isNotBlank(orgIdentifier)) {
      identifierRefBuilder.setOrgIdentifier(StringValue.of(orgIdentifier));
    }

    if (isNotBlank(projectIdentifier)) {
      identifierRefBuilder.setProjectIdentifier(StringValue.of(projectIdentifier));
    }
    return identifierRefBuilder.build();
  }

  public void deleteSecretEntityReferenceWhenSecretGetsDeleted(EncryptedDataDTO encryptedDataDTO) {
    String secretMangerFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(encryptedDataDTO.getAccount(),
        encryptedDataDTO.getOrg(), encryptedDataDTO.getProject(), encryptedDataDTO.getSecretManager());
    String secretFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(encryptedDataDTO.getAccount(),
        encryptedDataDTO.getOrg(), encryptedDataDTO.getProject(), encryptedDataDTO.getIdentifier());
    boolean entityReferenceDeleted = false;
    try {
      DeleteSetupUsageDTO deleteSetupUsageDTO = DeleteSetupUsageDTO.newBuilder()
                                                    .setAccountIdentifier(encryptedDataDTO.getAccount())
                                                    .setReferredByEntityFQN(secretFQN)
                                                    .setReferredEntityFQN(secretMangerFQN)
                                                    .build();
      eventProducer.send(Message.newBuilder()
                             .putMetadata("accountId", encryptedDataDTO.getAccount())
                             .setData(deleteSetupUsageDTO.toByteString())
                             .build());
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
