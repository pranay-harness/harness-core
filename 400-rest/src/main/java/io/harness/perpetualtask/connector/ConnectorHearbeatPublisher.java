package io.harness.perpetualtask.connector;

import static io.harness.NGConstants.CONNECTOR_HEARTBEAT_LOG_PREFIX;
import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.CONNECTORS;

import static software.wings.utils.Utils.emptyIfNull;

import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorHeartbeatDelegateResponse;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entityactivity.EntityActivityCreateDTO;
import io.harness.ng.core.activityhistory.NGActivityStatus;
import io.harness.ng.core.activityhistory.NGActivityType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ConnectorHearbeatPublisher {
  Producer eventProducer;
  IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;

  @Inject
  public ConnectorHearbeatPublisher(@Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer,
      IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper) {
    this.eventProducer = eventProducer;
    this.identifierRefProtoDTOHelper = identifierRefProtoDTOHelper;
  }

  private static final String CONNECTIVITY_CHECK_DESCRIPTION = "Connectivity Check";

  public void pushConnectivityCheckActivity(
      String accountId, ConnectorHeartbeatDelegateResponse heartbeatDelegateResponse) {
    if (heartbeatDelegateResponse == null) {
      log.error("{} got null delegate heartbeat response in the connector heartbeat for the account {}",
          CONNECTOR_HEARTBEAT_LOG_PREFIX, accountId);
      return;
    }
    EntityActivityCreateDTO ngActivityDTO = createConnectivityCheckActivityDTO(heartbeatDelegateResponse);
    try {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", accountId, EventsFrameworkMetadataConstants.ENTITY_TYPE,
                  EventsFrameworkMetadataConstants.ACTIVITY_ENTITY, EventsFrameworkMetadataConstants.ACTION,
                  EventsFrameworkMetadataConstants.CREATE_ACTION))
              .setData(ngActivityDTO.toByteString())
              .build());
    } catch (Exception ex) {
      log.error("{} Exception while pushing the heartbeat result {}", CONNECTOR_HEARTBEAT_LOG_PREFIX,
          String.format(CONNECTOR_STRING, heartbeatDelegateResponse.getIdentifier(),
              heartbeatDelegateResponse.getAccountIdentifier(), heartbeatDelegateResponse.getOrgIdentifier(),
              heartbeatDelegateResponse.getProjectIdentifier()));
    }
  }

  private EntityActivityCreateDTO createConnectivityCheckActivityDTO(
      @NotNull ConnectorHeartbeatDelegateResponse heartbeatDelegateResponse) {
    IdentifierRefProtoDTO identifierRefProtoDTO = identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(
        heartbeatDelegateResponse.getAccountIdentifier(), heartbeatDelegateResponse.getOrgIdentifier(),
        heartbeatDelegateResponse.getProjectIdentifier(), heartbeatDelegateResponse.getIdentifier());
    EntityDetailProtoDTO referredEntity = EntityDetailProtoDTO.newBuilder()
                                              .setType(CONNECTORS)
                                              .setIdentifierRef(identifierRefProtoDTO)
                                              .setName(emptyIfNull(heartbeatDelegateResponse.getName()))
                                              .build();
    NGActivityStatus activityStatus = NGActivityStatus.FAILED;
    if (heartbeatDelegateResponse.getConnectorValidationResult() != null
        && heartbeatDelegateResponse.getConnectorValidationResult().getStatus() == ConnectivityStatus.SUCCESS) {
      activityStatus = NGActivityStatus.SUCCESS;
    }
    String errorMessage = null;
    if (heartbeatDelegateResponse.getConnectorValidationResult() != null) {
      errorMessage = heartbeatDelegateResponse.getConnectorValidationResult().getErrorSummary();
    }
    EntityActivityCreateDTO.Builder builder =
        EntityActivityCreateDTO.newBuilder()
            .setType(NGActivityType.CONNECTIVITY_CHECK.toString())
            .setStatus(activityStatus.toString())
            .setActivityTime(heartbeatDelegateResponse.getConnectorValidationResult().getTestedAt())
            .setAccountIdentifier(heartbeatDelegateResponse.getAccountIdentifier())
            .setDescription(CONNECTIVITY_CHECK_DESCRIPTION)
            .setReferredEntity(referredEntity);
    if (errorMessage != null) {
      builder.setErrorMessage(errorMessage);
    }
    return builder.build();
  }
}
