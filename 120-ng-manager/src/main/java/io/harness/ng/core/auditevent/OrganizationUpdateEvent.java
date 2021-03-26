package io.harness.ng.core.auditevent;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.ORGANIZATION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.OrganizationDTO;

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

  public ResourceScope getResourceScope() {
    return new OrgScope(accountIdentifier, newOrganization.getIdentifier());
  }

  public Resource getResource() {
    return Resource.builder().identifier(newOrganization.getIdentifier()).type(ORGANIZATION).build();
  }

  public String getEventType() {
    return "OrganizationUpdated";
  }
}
