package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.ORGANIZATION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.OrganizationDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class OrganizationRestoreEvent implements Event {
  private OrganizationDTO organization;
  private String accountIdentifier;

  public OrganizationRestoreEvent(String accountIdentifier, OrganizationDTO organization) {
    this.organization = organization;
    this.accountIdentifier = accountIdentifier;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return new OrgScope(accountIdentifier, organization.getIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, organization.getName());
    return Resource.builder().identifier(organization.getIdentifier()).type(ORGANIZATION).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "OrganizationRestored";
  }
}
