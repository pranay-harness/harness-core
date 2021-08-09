package io.harness.gitsync.common.events;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.NgEventLogContext;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.core.fullsync.FullSyncTriggerService;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.event.MessageListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class FullSyncMessageListener implements MessageListener {
  private final FullSyncTriggerService fullSyncTriggerService;

  @Override
  public boolean handleMessage(Message message) {
    // todo(abhinav): make idempotent
    final String messageId = message.getId();
    log.info("Processing the Full Sync event with the id {}", messageId);
    try (AutoLogContext ignore1 = new NgEventLogContext(messageId, OVERRIDE_ERROR)) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (!metadataMap.containsKey(GitSyncConfigChangeEventConstants.CONFIG_SWITCH_TYPE)) {
        log.error("Cannot find message metadata map for config change event hence ignoring.");
        return true;
      }
      final String configSwitchType = metadataMap.get(GitSyncConfigChangeEventConstants.CONFIG_SWITCH_TYPE);
      if (GitSyncConfigSwitchType.ENABLED.name().equals(configSwitchType)) {
        final EntityScopeInfo entityScopeInfo = getEntityScopeInfo(message);
        fullSyncTriggerService.triggerFullSync(entityScopeInfo);
      }
      return true;
    }
  }

  private EntityScopeInfo getEntityScopeInfo(Message message) {
    try {
      return EntityScopeInfo.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException("Unable to parse entity scope info", e);
    }
  }
}
