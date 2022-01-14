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
public class OrganizationUpdateEvent implements Event {
  private OrganizationDTO newOrganization;
  private OrganizationDTO oldOrganization;
  private String accountIdentifier;

  public OrganizationUpdateEvent(
      String accountIdentifier, OrganizationDTO newOrganization, OrganizationDTO oldOrganization) {
    this.newOrganization = newOrganization;
    this.oldOrganization = oldOrganization;
    this.accountIdentifier = accountIdentifier;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return new OrgScope(accountIdentifier, newOrganization.getIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, newOrganization.getName());
    return Resource.builder().identifier(newOrganization.getIdentifier()).type(ORGANIZATION).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "OrganizationUpdated";
  }
}
