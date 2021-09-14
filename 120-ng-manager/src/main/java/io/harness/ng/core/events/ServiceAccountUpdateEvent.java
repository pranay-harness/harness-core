/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.SERVICE_ACCOUNT;

import io.harness.annotations.dev.OwnedBy;
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
    return Resource.builder().identifier(oldServiceAccount.getIdentifier()).type(SERVICE_ACCOUNT).build();
  }

  @Override
  @JsonIgnore
  public String getEventType() {
    return SERVICE_ACCOUNT_UPDATED;
  }
}
