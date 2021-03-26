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
public class OrganizationDeleteEvent implements Event {
  private OrganizationDTO organization;
  private String accountIdentifier;

  public OrganizationDeleteEvent(String accountIdentifier, OrganizationDTO organization) {
    this.organization = organization;
    this.accountIdentifier = accountIdentifier;
  }

  public ResourceScope getResourceScope() {
    return new OrgScope(accountIdentifier, organization.getIdentifier());
  }

  public Resource getResource() {
    return Resource.builder().identifier(organization.getIdentifier()).type(ORGANIZATION).build();
  }

  public String getEventType() {
    return "OrganizationDeleted";
  }
}
