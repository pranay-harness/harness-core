package io.harness.cvng.core.jobs;

import io.harness.beans.IdentifierRef;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.connector.ConnectorEntityChangeDTO;
import io.harness.utils.IdentifierRefHelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ConnectorChangeEventMessageProcessor implements ConsumerMessageProcessor {
  @Inject private CVConfigService cvConfigService;
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Override
  public void processMessage(Message message) {
    Preconditions.checkState(validateMessage(message), "Invalid message received by Connector Change Event Processor");

    ConnectorEntityChangeDTO connectorEntityChangeDTO;
    try {
      connectorEntityChangeDTO = ConnectorEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking ConnectorEntityChangeDTO for key {}", message.getId(), e);
      throw new IllegalStateException(e);
    }

    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap.containsKey(EventsFrameworkConstants.ACTION_METADATA)) {
      switch (metadataMap.get(EventsFrameworkConstants.ACTION_METADATA)) {
        case EventsFrameworkConstants.UPDATE_ACTION:
          processUpdateAction(connectorEntityChangeDTO);
          return;
        default:
      }
    }
  }
  @VisibleForTesting
  void processUpdateAction(ConnectorEntityChangeDTO connectorEntityChangeDTO) {
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRefFromEntityIdentifiers(
        connectorEntityChangeDTO.getIdentifier().getValue(), connectorEntityChangeDTO.getAccountIdentifier().getValue(),
        connectorEntityChangeDTO.getOrgIdentifier().getValue(),
        connectorEntityChangeDTO.getProjectIdentifier().getValue());

    log.info("IdentifierRef {}", identifierRef.getScope().getYamlRepresentation() + "," + identifierRef);

    List<CVConfig> cvConfigsWithConnector =
        cvConfigService.findByConnectorIdentifier(connectorEntityChangeDTO.getAccountIdentifier().getValue(),
            connectorEntityChangeDTO.getOrgIdentifier().getValue(),
            connectorEntityChangeDTO.getProjectIdentifier().getValue(),
            connectorEntityChangeDTO.getIdentifier().getValue(), identifierRef.getScope());
    cvConfigsWithConnector.forEach(cvConfig -> {
      dataCollectionTaskService.resetLiveMonitoringPerpetualTask(cvConfig);
      resetVerificationJobPerpetualTasks(cvConfig);
    });
  }
  private void resetVerificationJobPerpetualTasks(CVConfig cvConfig) {
    List<String> verificationJobInstanceIds =
        verificationTaskService.getAllVerificationJobInstanceIdsForCVConfig(cvConfig.getUuid());
    List<VerificationJobInstance> verificationJobInstances =
        verificationJobInstanceService.filterRunningVerificationJobInstances(verificationJobInstanceIds);
    verificationJobInstances.forEach(verificationJobInstance
        -> verificationJobInstanceService.resetPerpetualTask(verificationJobInstance, cvConfig));
  }

  private boolean validateMessage(Message message) {
    return message != null && message.hasMessage() && message.getMessage().getMetadataMap() != null
        && message.getMessage().getMetadataMap().containsKey(EventsFrameworkConstants.ENTITY_TYPE_METADATA)
        && EventsFrameworkConstants.CONNECTOR_ENTITY.equals(
            message.getMessage().getMetadataMap().get(EventsFrameworkConstants.ENTITY_TYPE_METADATA));
  }
}
