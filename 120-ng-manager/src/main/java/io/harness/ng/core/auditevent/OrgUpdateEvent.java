package io.harness.ng.core.auditevent;

import static io.harness.audit.ResourceTypeConstants.ORGANIZATION;

import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.OrganizationDTO;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrgUpdateEvent implements Event {
  private OrganizationDTO newOrg;
  private OrganizationDTO oldOrg;
  private String accountIdentifier;

  public OrgUpdateEvent(String accountIdentifier, OrganizationDTO newOrg, OrganizationDTO oldOrg) {
    this.newOrg = newOrg;
    this.oldOrg = oldOrg;
    this.accountIdentifier = accountIdentifier;
  }

  public ResourceScope getResourceScope() {
    return new AccountScope(accountIdentifier);
  }

  public Resource getResource() {
    return Resource.builder().identifier(newOrg.getIdentifier()).type(ORGANIZATION).build();
  }

  public String getEventType() {
    return "OrgUpdated";
  }
}
