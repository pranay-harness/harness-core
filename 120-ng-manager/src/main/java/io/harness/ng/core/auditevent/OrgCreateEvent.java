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
public class OrgCreateEvent implements Event {
  private OrganizationDTO org;
  private String accountIdentifier;

  public OrgCreateEvent(String accountIdentifier, OrganizationDTO newOrg) {
    this.org = newOrg;
    this.accountIdentifier = accountIdentifier;
  }

  public ResourceScope getResourceScope() {
    return new AccountScope(accountIdentifier);
  }

  public Resource getResource() {
    return Resource.builder().identifier(org.getIdentifier()).type(ORGANIZATION).build();
  }

  public String getEventType() {
    return "OrgCreated";
  }
}
