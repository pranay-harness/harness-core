package io.harness.ng.core.event;

import static io.harness.EntityCRUDEventsConstants.ACTION_METADATA;
import static io.harness.EntityCRUDEventsConstants.CREATE_ACTION;
import static io.harness.EntityCRUDEventsConstants.DELETE_ACTION;
import static io.harness.EntityCRUDEventsConstants.ENTITY_TYPE_METADATA;
import static io.harness.EntityCRUDEventsConstants.PROJECT_ENTITY;
import static io.harness.exception.WingsException.USER;

import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.project.ProjectEntityChangeDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ProjectChangeEventMessageProcessor implements ConsumerMessageProcessor {
  private final HarnessSMManager harnessSMManager;

  @Inject
  public ProjectChangeEventMessageProcessor(HarnessSMManager harnessSMManager) {
    this.harnessSMManager = harnessSMManager;
  }

  @Override
  public void processMessage(Message message) {
    if (!validateMessage(message)) {
      log.error("Invalid message received by Project Change Event Processor");
      return;
    }

    ProjectEntityChangeDTO projectEntityChangeDTO;
    try {
      projectEntityChangeDTO = ProjectEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking ProjectEntityChangeDTO for key {}", message.getId(), e);
      return;
    }

    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap.containsKey(ACTION_METADATA)) {
      switch (metadataMap.get(ACTION_METADATA)) {
        case CREATE_ACTION:
          processCreateAction(projectEntityChangeDTO);
          return;
        case DELETE_ACTION:
          processDeleteAction(projectEntityChangeDTO);
          return;
        default:
      }
    }
  }

  private void processCreateAction(ProjectEntityChangeDTO projectEntityChangeDTO) {
    try {
      harnessSMManager.createHarnessSecretManager(projectEntityChangeDTO.getAccountIdentifier(),
          projectEntityChangeDTO.getOrgIdentifier(), projectEntityChangeDTO.getIdentifier());
    } catch (Exception ex) {
      log.error(
          String.format(
              "Harness Secret Manager could not be created for accountIdentifier %s, orgIdentifier %s and projectIdentifier %s",
              projectEntityChangeDTO.getAccountIdentifier(), projectEntityChangeDTO.getOrgIdentifier(),
              projectEntityChangeDTO.getIdentifier()),
          ex, USER);
    }
  }

  private void processDeleteAction(ProjectEntityChangeDTO projectEntityChangeDTO) {
    try {
      if (!harnessSMManager.deleteHarnessSecretManager(projectEntityChangeDTO.getAccountIdentifier(),
              projectEntityChangeDTO.getOrgIdentifier(), projectEntityChangeDTO.getIdentifier())) {
        log.error(
            String.format(
                "Harness Secret Manager could not be deleted for accountIdentifier %s, orgIdentifier %s and projectIdentifier %s",
                projectEntityChangeDTO.getAccountIdentifier(), projectEntityChangeDTO.getOrgIdentifier(),
                projectEntityChangeDTO.getIdentifier()),
            USER);
      }
    } catch (Exception ex) {
      log.error(
          String.format(
              "Harness Secret Manager could not be deleted for accountIdentifier %s, orgIdentifier %s and projectIdentifier %s",
              projectEntityChangeDTO.getAccountIdentifier(), projectEntityChangeDTO.getOrgIdentifier(),
              projectEntityChangeDTO.getIdentifier()),
          ex, USER);
    }
  }

  private boolean validateMessage(Message message) {
    return message != null && message.hasMessage() && message.getMessage().getMetadataMap() != null
        && message.getMessage().getMetadataMap().containsKey(ENTITY_TYPE_METADATA)
        && PROJECT_ENTITY.equals(message.getMessage().getMetadataMap().get(ENTITY_TYPE_METADATA));
  }
}
