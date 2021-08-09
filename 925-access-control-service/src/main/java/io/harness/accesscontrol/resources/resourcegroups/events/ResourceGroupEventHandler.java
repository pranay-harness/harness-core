package io.harness.accesscontrol.resources.resourcegroups.events;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.resourcegroup.ResourceGroupEntityChangeDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@Singleton
@Slf4j
public class ResourceGroupEventHandler implements EventHandler {
  private final HarnessResourceGroupService harnessResourceGroupService;

  @Inject
  public ResourceGroupEventHandler(HarnessResourceGroupService harnessResourceGroupService) {
    this.harnessResourceGroupService = harnessResourceGroupService;
  }

  @Override
  public boolean handle(Message message) {
    ResourceGroupEntityChangeDTO resourceGroupEntityChangeDTO = null;
    try {
      resourceGroupEntityChangeDTO = ResourceGroupEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking ResourceGroupEntityChangeDTO for key {}", message.getId(), e);
    }
    if (Objects.isNull(resourceGroupEntityChangeDTO)) {
      return true;
    }
    try {
      HarnessScopeParams scopeParams =
          HarnessScopeParams.builder()
              .accountIdentifier(stripToNull(resourceGroupEntityChangeDTO.getAccountIdentifier()))
              .orgIdentifier(stripToNull(resourceGroupEntityChangeDTO.getOrgIdentifier()))
              .projectIdentifier(stripToNull(resourceGroupEntityChangeDTO.getProjectIdentifier()))
              .build();
      Scope scope = ScopeMapper.fromParams(scopeParams);
      if (getEventType(message).equals(DELETE_ACTION)) {
        harnessResourceGroupService.deleteIfPresent(stripToNull(resourceGroupEntityChangeDTO.getIdentifier()), scope);
      } else {
        harnessResourceGroupService.sync(stripToNull(resourceGroupEntityChangeDTO.getIdentifier()), scope);
      }
    } catch (Exception e) {
      log.error("Could not process the resource group change event {} due to error", resourceGroupEntityChangeDTO, e);
      return false;
    }
    return true;
  }

  private String getEventType(Message message) {
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    return metadataMap.get(ACTION);
  }
}
