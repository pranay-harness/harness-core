package io.harness.accesscontrol.principals.serviceaccounts.events;

import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.accesscontrol.principals.serviceaccounts.HarnessServiceAccountService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams.HarnessScopeParamsBuilder;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@Slf4j
@Singleton
public class ServiceAccountMembershipEventHandler implements EventHandler {
  private final HarnessServiceAccountService harnessServiceAccountService;
  private final ScopeService scopeService;

  @Inject
  public ServiceAccountMembershipEventHandler(
      HarnessServiceAccountService harnessServiceAccountService, ScopeService scopeService) {
    this.harnessServiceAccountService = harnessServiceAccountService;
    this.scopeService = scopeService;
  }

  @Override
  public boolean handle(Message message) {
    EntityChangeDTO entityChangeDTO = null;
    try {
      entityChangeDTO = EntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking EntityChangeDTO for user group event with key {}", message.getId(), e);
    }
    if (Objects.isNull(entityChangeDTO)) {
      return true;
    }
    try {
      HarnessScopeParamsBuilder builder =
          HarnessScopeParams.builder().accountIdentifier(entityChangeDTO.getAccountIdentifier().getValue());

      if (entityChangeDTO.getOrgIdentifier() != null) {
        builder.orgIdentifier(entityChangeDTO.getOrgIdentifier().getValue());
      }
      if (entityChangeDTO.getProjectIdentifier() != null) {
        builder.projectIdentifier(entityChangeDTO.getProjectIdentifier().getValue());
      }

      Scope scope = scopeService.buildScopeFromParams(builder.build());
      harnessServiceAccountService.sync(entityChangeDTO.getIdentifier().getValue(), scope);
    } catch (Exception e) {
      log.error("Could not process the resource group change event {} due to error", entityChangeDTO, e);
      return false;
    }
    return true;
  }
}
