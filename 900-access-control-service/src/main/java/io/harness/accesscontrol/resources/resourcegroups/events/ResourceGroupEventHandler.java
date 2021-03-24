package io.harness.accesscontrol.resources.resourcegroups.events;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;

import io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeParams;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.eventsframework.api.EventHandler;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.resourcegroup.ResourceGroupEntityChangeDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ResourceGroupEventHandler implements EventHandler {
  private final HarnessResourceGroupService harnessResourceGroupService;
  private final ScopeService scopeService;

  @Inject
  public ResourceGroupEventHandler(HarnessResourceGroupService harnessResourceGroupService, ScopeService scopeService) {
    this.harnessResourceGroupService = harnessResourceGroupService;
    this.scopeService = scopeService;
  }

  @Override
  public boolean handle(Message message) {
    ResourceGroupEntityChangeDTO resourceGroupEntityChangeDTO = null;
    try {
      resourceGroupEntityChangeDTO = ResourceGroupEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking ResourceGroupEntityChangeDTO for key {}", message.getId(), e);
    }
    String action = message.getMessage().getMetadataMap().get(ACTION);

    if (Objects.isNull(resourceGroupEntityChangeDTO)) {
      return true;
    }

    try {
      ScopeParams scopeParams = HarnessScopeParams.builder()
                                    .accountIdentifier(resourceGroupEntityChangeDTO.getAccountIdentifier())
                                    .orgIdentifier(resourceGroupEntityChangeDTO.getOrgIdentifier())
                                    .projectIdentifier(resourceGroupEntityChangeDTO.getProjectIdentifier())
                                    .build();
      Scope scope = scopeService.buildScopeFromParams(scopeParams);
      harnessResourceGroupService.sync(resourceGroupEntityChangeDTO.getIdentifier(), scope);
    } catch (Exception e) {
      log.error("Could not process the resource group change event {} due to error", resourceGroupEntityChangeDTO, e);
      return false;
    }
    return true;
  }
}
