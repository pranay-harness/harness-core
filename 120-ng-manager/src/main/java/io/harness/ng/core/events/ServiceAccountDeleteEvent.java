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
public class ServiceAccountDeleteEvent implements Event {
  public static final String SERVICE_ACCOUNT_DELETED = "ServiceAccountDeleted";
  private ServiceAccountDTO serviceAccount;

  public ServiceAccountDeleteEvent(ServiceAccountDTO serviceAccountDTO) {
    this.serviceAccount = serviceAccountDTO;
  }

  @Override
  @JsonIgnore
  public ResourceScope getResourceScope() {
    return ResourceScopeMapper.getResourceScope(Scope.of(serviceAccount.getAccountIdentifier(),
        serviceAccount.getOrgIdentifier(), serviceAccount.getProjectIdentifier()));
  }

  @Override
  @JsonIgnore
  public Resource getResource() {
    return Resource.builder()
        .identifier(serviceAccount.getIdentifier())
        .type(ResourceType.SERVICE_ACCOUNT.name())
        .build();
  }

  @Override
  @JsonIgnore
  public String getEventType() {
    return SERVICE_ACCOUNT_DELETED;
  }
}
