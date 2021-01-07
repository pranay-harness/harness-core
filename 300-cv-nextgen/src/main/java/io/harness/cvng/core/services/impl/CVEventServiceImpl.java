package io.harness.cvng.core.services.impl;

import static io.harness.NGConstants.ENTITY_REFERENCE_LOG_PREFIX;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;

import io.harness.beans.IdentifierRef;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVEventService;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entitysetupusage.DeleteSetupUsageDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class CVEventServiceImpl implements CVEventService {
  @Inject @Named(ENTITY_CRUD) private AbstractProducer eventProducer;
  @Inject private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;

  @Override
  public void sendConnectorCreateEvent(CVConfig cvConfig) {
    String cvConfigConnectorFQN = getCVConfigFullyQualifiedName(cvConfig, cvConfig.getConnectorIdentifier());
    String cvConfigFQN = getCVConfigFullyQualifiedName(cvConfig, cvConfig.getIdentifier());
    IdentifierRefProtoDTO configReference = getIdentifierRefProtoDTOFromConfig(cvConfig);
    IdentifierRef identifierRef = getIdentifierRef(cvConfig, cvConfig.getConnectorIdentifier());
    IdentifierRefProtoDTO configReferenceConnector = getIdentifierRefProtoDTOFromIdentifierRef(identifierRef);
    EntitySetupUsageCreateDTO connectorEntityReferenceDTO = getEntitySetupUsageCreateDTO(
        cvConfig, configReference, configReferenceConnector, EntityTypeProtoEnum.CONNECTORS);

    try {
      sendEventWithMessageForCreation(cvConfig, connectorEntityReferenceDTO);

    } catch (Exception ex) {
      log.info(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not created when the CV Config [{}] was created from [{}] connector",
          cvConfigFQN, cvConfigConnectorFQN);
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void sendConnectorDeleteEvent(CVConfig cvConfig) {
    IdentifierRef identifierRef = getIdentifierRef(cvConfig, cvConfig.getConnectorIdentifier());
    String cvConfigConnectorFQN = getCvConfigFQNFromIdentifierRef(identifierRef);
    String cvConfigFQN = getCVConfigFullyQualifiedName(cvConfig, cvConfig.getIdentifier());

    try {
      DeleteSetupUsageDTO deleteSetupUsageDTO = getDeleteSetupUsageDTO(cvConfig, cvConfigConnectorFQN, cvConfigFQN);

      sendEventWithMessageForDeletion(cvConfig, deleteSetupUsageDTO);
    } catch (Exception ex) {
      log.error(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not deleted when the CV Config [{}] was deleted from [{}] with the exception [{}] ",
          cvConfigFQN, cvConfigConnectorFQN, ex.getMessage());
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void sendServiceCreateEvent(CVConfig cvConfig) {
    String cvConfigServiceFQN = getCVConfigFullyQualifiedName(cvConfig, cvConfig.getServiceIdentifier());
    String cvConfigFQN = getCVConfigFullyQualifiedName(cvConfig, cvConfig.getIdentifier());
    IdentifierRefProtoDTO configReference = getIdentifierRefProtoDTOFromConfig(cvConfig);
    IdentifierRef identifierRef = getIdentifierRef(cvConfig, cvConfig.getServiceIdentifier());
    IdentifierRefProtoDTO configReferenceService = getIdentifierRefProtoDTOFromIdentifierRef(identifierRef);
    EntitySetupUsageCreateDTO serviceEntityReferenceDTO =
        getEntitySetupUsageCreateDTO(cvConfig, configReference, configReferenceService, EntityTypeProtoEnum.SERVICE);

    try {
      sendEventWithMessageForCreation(cvConfig, serviceEntityReferenceDTO);
    } catch (Exception ex) {
      log.info(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not created when the CV Config [{}] was created from [{}] service",
          cvConfigFQN, cvConfigServiceFQN);
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void sendServiceDeleteEvent(CVConfig cvConfig) {
    IdentifierRef identifierRef = getIdentifierRef(cvConfig, cvConfig.getServiceIdentifier());
    String cvConfigServiceFQN = getCvConfigFQNFromIdentifierRef(identifierRef);
    String cvConfigFQN = getCVConfigFullyQualifiedName(cvConfig, cvConfig.getIdentifier());

    try {
      DeleteSetupUsageDTO deleteSetupUsageDTO = getDeleteSetupUsageDTO(cvConfig, cvConfigServiceFQN, cvConfigFQN);

      sendEventWithMessageForDeletion(cvConfig, deleteSetupUsageDTO);
    } catch (Exception ex) {
      log.error(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not deleted when the CV Config [{}] was deleted from [{}] with the exception [{}] ",
          cvConfigFQN, cvConfigServiceFQN, ex.getMessage());
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void sendEnvironmentCreateEvent(CVConfig cvConfig) {
    String cvConfigEnvironmentFQN = getCVConfigFullyQualifiedName(cvConfig, cvConfig.getEnvIdentifier());
    String cvConfigFQN = getCVConfigFullyQualifiedName(cvConfig, cvConfig.getIdentifier());
    IdentifierRefProtoDTO configReference = getIdentifierRefProtoDTOFromConfig(cvConfig);
    IdentifierRef identifierRef = getIdentifierRef(cvConfig, cvConfig.getEnvIdentifier());
    IdentifierRefProtoDTO configReferenceEnvironment = getIdentifierRefProtoDTOFromIdentifierRef(identifierRef);
    EntitySetupUsageCreateDTO environmentEntityReferenceDTO = getEntitySetupUsageCreateDTO(
        cvConfig, configReference, configReferenceEnvironment, EntityTypeProtoEnum.ENVIRONMENT);

    try {
      sendEventWithMessageForCreation(cvConfig, environmentEntityReferenceDTO);

    } catch (Exception ex) {
      log.info(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not created when the CV Config [{}] was created from [{}] environment",
          cvConfigFQN, cvConfigEnvironmentFQN);
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void sendEnvironmentDeleteEvent(CVConfig cvConfig) {
    IdentifierRef identifierRef = getIdentifierRef(cvConfig, cvConfig.getEnvIdentifier());
    String cvConfigEnvironmentFQN = getCvConfigFQNFromIdentifierRef(identifierRef);
    String cvConfigFQN = getCVConfigFullyQualifiedName(cvConfig, cvConfig.getIdentifier());

    try {
      DeleteSetupUsageDTO deleteSetupUsageDTO = getDeleteSetupUsageDTO(cvConfig, cvConfigEnvironmentFQN, cvConfigFQN);

      sendEventWithMessageForDeletion(cvConfig, deleteSetupUsageDTO);
    } catch (Exception ex) {
      log.error(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not deleted when the CV Config [{}] was deleted from [{}] with the exception [{}] ",
          cvConfigFQN, cvConfigEnvironmentFQN, ex.getMessage());
      throw new IllegalStateException(ex);
    }
  }

  private void sendEventWithMessageForCreation(CVConfig cvConfig, EntitySetupUsageCreateDTO connectorEntityReferenceDTO)
      throws ProducerShutdownException {
    eventProducer.send(
        Message.newBuilder()
            .putAllMetadata(ImmutableMap.of("accountId", cvConfig.getAccountId(),
                EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY,
                EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.CREATE_ACTION))
            .setData(connectorEntityReferenceDTO.toByteString())
            .build());
  }

  private void sendEventWithMessageForDeletion(CVConfig cvConfig, DeleteSetupUsageDTO deleteSetupUsageDTO)
      throws ProducerShutdownException {
    eventProducer.send(
        Message.newBuilder()
            .putAllMetadata(ImmutableMap.of("accountId", cvConfig.getAccountId(),
                EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY,
                EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.DELETE_ACTION))
            .setData(deleteSetupUsageDTO.toByteString())
            .build());
  }

  private IdentifierRefProtoDTO getIdentifierRefProtoDTOFromIdentifierRef(IdentifierRef identifierRef) {
    return identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(identifierRef.getAccountIdentifier(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
  }

  private IdentifierRefProtoDTO getIdentifierRefProtoDTOFromConfig(CVConfig cvConfig) {
    return identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(cvConfig.getAccountId(), cvConfig.getOrgIdentifier(),
        cvConfig.getProjectIdentifier(), cvConfig.getIdentifier());
  }

  private IdentifierRef getIdentifierRef(CVConfig cvConfig, String scopedIdentifier) {
    return IdentifierRefHelper.getIdentifierRef(
        scopedIdentifier, cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());
  }

  private String getCVConfigFullyQualifiedName(CVConfig cvConfig, String scopedIdentifier) {
    return FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), scopedIdentifier);
  }

  private String getCvConfigFQNFromIdentifierRef(IdentifierRef identifierRef) {
    return FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(identifierRef.getAccountIdentifier(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
  }

  @NotNull
  private DeleteSetupUsageDTO getDeleteSetupUsageDTO(CVConfig cvConfig, String cvConfigScopedFQN, String cvConfigFQN) {
    return DeleteSetupUsageDTO.newBuilder()
        .setAccountIdentifier(cvConfig.getAccountId())
        .setReferredByEntityFQN(cvConfigFQN)
        .setReferredEntityFQN(cvConfigScopedFQN)
        .build();
  }

  @NotNull
  private EntitySetupUsageCreateDTO getEntitySetupUsageCreateDTO(CVConfig cvConfig,
      IdentifierRefProtoDTO configReference, IdentifierRefProtoDTO configReferenceEnvironment,
      EntityTypeProtoEnum typeProtoEnum) {
    EntityDetailProtoDTO configDetails = EntityDetailProtoDTO.newBuilder()
                                             .setIdentifierRef(configReference)
                                             .setType(EntityTypeProtoEnum.CV_CONFIG)
                                             .setName(cvConfig.getMonitoringSourceName())
                                             .build();

    EntityDetailProtoDTO scopedManagerDetails =
        EntityDetailProtoDTO.newBuilder().setIdentifierRef(configReferenceEnvironment).setType(typeProtoEnum).build();

    return EntitySetupUsageCreateDTO.newBuilder()
        .setAccountIdentifier(cvConfig.getAccountId())
        .setReferredByEntity(configDetails)
        .setReferredEntity(scopedManagerDetails)
        .build();
  }
}
