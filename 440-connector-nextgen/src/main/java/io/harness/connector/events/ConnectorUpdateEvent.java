package io.harness.connector.events;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.audit.ResourceTypeConstants.CONNECTOR;
import static io.harness.connector.ConnectorEvent.CONNECTOR_UPDATED;
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
public class ConnectorUpdateEvent implements Event {
  private ConnectorInfoDTO oldConnector;
  private ConnectorInfoDTO newConnector;
  private String accountIdentifier;

  public ConnectorUpdateEvent(String accountIdentifier, ConnectorInfoDTO oldConnector, ConnectorInfoDTO newConnector) {
    this.accountIdentifier = accountIdentifier;
    this.oldConnector = oldConnector;
    this.newConnector = newConnector;
  }
  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (isNotEmpty(newConnector.getOrgIdentifier())) {
      if (isEmpty(newConnector.getProjectIdentifier())) {
        return new OrgScope(accountIdentifier, newConnector.getOrgIdentifier());
      } else {
        return new ProjectScope(
            accountIdentifier, newConnector.getOrgIdentifier(), newConnector.getProjectIdentifier());
      }
    }
    return new AccountScope(accountIdentifier);
  }
  @JsonIgnore
  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, newConnector.getName());
    return Resource.builder().identifier(newConnector.getIdentifier()).type(CONNECTOR).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return CONNECTOR_UPDATED;
  }
}
