package io.harness.connector.events;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.audit.ResourceTypeConstants.CONNECTOR;
import static io.harness.connector.ConnectorEvent.CONNECTOR_DELETED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(DX)
@Getter
@NoArgsConstructor
public class ConnectorDeleteEvent implements Event {
  private String accountIdentifier;
  private ConnectorInfoDTO connectorDTO;

  public ConnectorDeleteEvent(String accountIdentifier, ConnectorInfoDTO connectorDTO) {
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
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, connectorDTO.getName());
    return Resource.builder().identifier(connectorDTO.getIdentifier()).type(CONNECTOR).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return CONNECTOR_DELETED;
  }
}
