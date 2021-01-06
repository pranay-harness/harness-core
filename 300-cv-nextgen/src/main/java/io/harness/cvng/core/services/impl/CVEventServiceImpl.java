package io.harness.cvng.core.services.impl;
import static io.harness.NGConstants.ENTITY_REFERENCE_LOG_PREFIX;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;

import io.harness.beans.IdentifierRef;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVEventService;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.AbstractProducer;
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

@Slf4j
public class CVEventServiceImpl implements CVEventService {
  @Inject @Named(ENTITY_CRUD) private AbstractProducer eventProducer;
  @Inject private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;

  @Override
  public void sendConnectorCreateEvent(CVConfig cvConfig) {
    String cvConfigConnectorFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(cvConfig.getAccountId(),
        cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), cvConfig.getConnectorIdentifier());
    String cvConfigFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(cvConfig.getAccountId(),
        cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), cvConfig.getIdentifier());

    IdentifierRefProtoDTO configReference =
        identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(cvConfig.getAccountId(), cvConfig.getOrgIdentifier(),
            cvConfig.getProjectIdentifier(), cvConfig.getIdentifier());

    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(cvConfig.getConnectorIdentifier(),
        cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());

    IdentifierRefProtoDTO configReferenceConnector =
        identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(identifierRef.getAccountIdentifier(),
            identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());

    EntityDetailProtoDTO configDetails = EntityDetailProtoDTO.newBuilder()
                                             .setIdentifierRef(configReference)
                                             .setType(EntityTypeProtoEnum.CV_CONFIG)
                                             .setName(cvConfig.getMonitoringSourceName())
                                             .build();

    EntityDetailProtoDTO connectorManagerDetails = EntityDetailProtoDTO.newBuilder()
                                                       .setIdentifierRef(configReferenceConnector)
                                                       .setType(EntityTypeProtoEnum.CONNECTORS)
                                                       .build();

    EntitySetupUsageCreateDTO entityReferenceDTO = EntitySetupUsageCreateDTO.newBuilder()
                                                       .setAccountIdentifier(cvConfig.getAccountId())
                                                       .setReferredByEntity(configDetails)
                                                       .setReferredEntity(connectorManagerDetails)
                                                       .build();

    try {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", cvConfig.getAccountId(),
                  EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY,
                  EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.CREATE_ACTION))
              .setData(entityReferenceDTO.toByteString())
              .build());
    } catch (Exception ex) {
      log.info(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not created when the CV Config [{}] was created from [{}] connector",
          cvConfigFQN, cvConfigConnectorFQN);
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void sendConnectorDeleteEvent(CVConfig cvConfig) {
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(cvConfig.getConnectorIdentifier(),
        cvConfig.getAccountId(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());

    String cvConfigConnectorFQN =
        FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(identifierRef.getAccountIdentifier(),
            identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());

    String cvConfigFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(cvConfig.getAccountId(),
        cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier(), cvConfig.getIdentifier());
    try {
      DeleteSetupUsageDTO deleteSetupUsageDTO = DeleteSetupUsageDTO.newBuilder()
                                                    .setAccountIdentifier(cvConfig.getAccountId())
                                                    .setReferredByEntityFQN(cvConfigFQN)
                                                    .setReferredEntityFQN(cvConfigConnectorFQN)
                                                    .build();

      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", cvConfig.getAccountId(),
                  EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY,
                  EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.DELETE_ACTION))
              .setData(deleteSetupUsageDTO.toByteString())
              .build());
    } catch (Exception ex) {
      log.error(ENTITY_REFERENCE_LOG_PREFIX
              + "The entity reference was not deleted when the CV Config [{}] was deleted from [{}] with the exception [{}] ",
          cvConfigFQN, cvConfigConnectorFQN, ex.getMessage());
      throw new IllegalStateException(ex);
    }
  }
}
