/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.events;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.audit.ResourceTypeConstants.CONNECTOR;
import static io.harness.connector.ConnectorEvent.CONNECTOR_CREATED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(DX)
@Getter
@NoArgsConstructor
public class ConnectorCreateEvent implements Event {
  private String accountIdentifier;
  private ConnectorInfoDTO connectorDTO;

  public ConnectorCreateEvent(String accountIdentifier, ConnectorInfoDTO connectorDTO) {
    this.accountIdentifier = accountIdentifier;
    this.connectorDTO = connectorDTO;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (isNotEmpty(connectorDTO.getOrgIdentifier())) {
      if (isEmpty(connectorDTO.getProjectIdentifier())) {
        return new OrgScope(accountIdentifier, connectorDTO.getOrgIdentifier());
      } else {
        return new ProjectScope(
            accountIdentifier, connectorDTO.getOrgIdentifier(), connectorDTO.getProjectIdentifier());
      }
    }
    return new AccountScope(accountIdentifier);
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder().identifier(connectorDTO.getIdentifier()).type(CONNECTOR).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return CONNECTOR_CREATED;
  }
}
