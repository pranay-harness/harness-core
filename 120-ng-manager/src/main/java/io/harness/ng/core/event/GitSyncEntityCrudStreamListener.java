package io.harness.ng.core.event;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CONNECTOR_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.UPDATE_ACTION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.gitsync.common.service.DecryptedScmKeySource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.DX)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class GitSyncEntityCrudStreamListener implements MessageListener {
  private final DecryptedScmKeySource decryptedScmKeySource;

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.get(ENTITY_TYPE) != null) {
        String entityType = metadataMap.get(ENTITY_TYPE);
        if (!entityType.equals(CONNECTOR_ENTITY)) {
          return false;
        }
        final String action = metadataMap.get(ACTION);

        switch (action) {
          case UPDATE_ACTION:
            return handleUpdateAction(message);
          default:
            return false;
        }
      }
    }
    return true;
  }

  private boolean handleUpdateAction(Message message) {
    EntityChangeDTO entityChangeDTO = null;
    try {
      entityChangeDTO = EntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Invalid entity change dto.", e);
      return false;
    }
    final IdentifierRef identifierRef =
        IdentifierRef.builder()
            .identifier(entityChangeDTO.getIdentifier().getValue())
            .accountIdentifier(entityChangeDTO.getAccountIdentifier().getValue())
            .orgIdentifier(
                entityChangeDTO.getOrgIdentifier() == null ? null : entityChangeDTO.getOrgIdentifier().getValue())
            .projectIdentifier(entityChangeDTO.getProjectIdentifier() == null
                    ? null
                    : entityChangeDTO.getProjectIdentifier().getValue())
            .build();
    decryptedScmKeySource.removeKey(identifierRef);
    return true;
  }
}
