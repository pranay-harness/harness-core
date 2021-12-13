package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceType;
import io.harness.beans.Scope;
import io.harness.event.Event;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.mapper.ResourceScopeMapper;
import io.harness.serviceaccount.ServiceAccountDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class ServiceAccountUpdateEvent implements Event {
  public static final String SERVICE_ACCOUNT_UPDATED = "ServiceAccountUpdated";
  private ServiceAccountDTO oldServiceAccount;
  private ServiceAccountDTO newServiceAccount;

  public ServiceAccountUpdateEvent(ServiceAccountDTO oldServiceAccount, ServiceAccountDTO newServiceAccount) {
    this.oldServiceAccount = oldServiceAccount;
    this.newServiceAccount = newServiceAccount;
  }

  @Override
  @JsonIgnore
  public ResourceScope getResourceScope() {
    return ResourceScopeMapper.getResourceScope(Scope.of(newServiceAccount.getAccountIdentifier(),
        newServiceAccount.getOrgIdentifier(), newServiceAccount.getProjectIdentifier()));
  }

  @Override
  @JsonIgnore
  public Resource getResource() {
    return Resource.builder()
        .identifier(oldServiceAccount.getIdentifier())
        .type(ResourceType.SERVICE_ACCOUNT.name())
        .build();
  }

  @Override
  @JsonIgnore
  public String getEventType() {
    return SERVICE_ACCOUNT_UPDATED;
  }
}
