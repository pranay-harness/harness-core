package io.harness.gitsync.common.events;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.eventsframework.NgEventLogContext;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.event.MessageListener;

import com.google.protobuf.StringValue;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BranchSyncEventListener implements MessageListener {
  YamlGitConfigService yamlGitConfigService;
  GitBranchService gitBranchService;

  @Override
  public boolean handleMessage(Message message) {
    String messageId = message.getId();
    log.info("Processing the branch Sync event with the id {}", messageId);
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    try (AutoLogContext ignore1 = new NgEventLogContext(messageId, OVERRIDE_ERROR)) {
      if (metadataMap.containsKey(GitSyncConfigChangeEventConstants.EVENT_TYPE)) {
        final String eventType = metadataMap.get(GitSyncConfigChangeEventConstants.EVENT_TYPE);
        if (eventType.equals(GitSyncConfigChangeEventType.SAVE_EVENT.name())) {
          final YamlGitConfigDTO yamlGitConfigDTO = getYamlGitConfigDTO(message);
          syncBranches(yamlGitConfigDTO);
          return true;
        }
      } else {
        log.info("No event type found");
        return true;
      }

      log.info("Completed branch Sync");
      return true;
    } catch (Exception ex) {
      log.info("Error processing the git branch sync for msg with id [{}]", messageId, ex);
    }
    return false;
  }

  private void syncBranches(YamlGitConfigDTO yamlGitConfigDTO) {
    gitBranchService.createBranches(yamlGitConfigDTO.getAccountIdentifier(),
        yamlGitConfigDTO.getOrganizationIdentifier(), yamlGitConfigDTO.getProjectIdentifier(),
        yamlGitConfigDTO.getGitConnectorRef(), yamlGitConfigDTO.getRepo(), yamlGitConfigDTO.getIdentifier());
  }

  private YamlGitConfigDTO getYamlGitConfigDTO(Message message) {
    final EntityScopeInfo entityScopeInfo = getEntityScopeInfo(message);
    return yamlGitConfigService.get(getStringFromStringValue(entityScopeInfo.getProjectId()),
        getStringFromStringValue(entityScopeInfo.getOrgId()), entityScopeInfo.getAccountId(),
        entityScopeInfo.getAccountId(), entityScopeInfo.getEntityIdentifier());
  }

  private String getStringFromStringValue(StringValue stringValue) {
    if (stringValue != null) {
      return stringValue.getValue();
    } else {
      return null;
    }
  }

  private EntityScopeInfo getEntityScopeInfo(Message message) {
    try {
      return EntityScopeInfo.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking EntityScopeInfo for key [{}]", message.getId(), e);
      throw new InvalidRequestException("Cannot deserialize EntityScopeInfo", e);
    }
  }
}
