package io.harness.resourcegroup.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.RESOURCE_GROUP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;

import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class ResourceGroupDeleteEvent implements Event {
  String accountIdentifier;
  ResourceGroupDTO resourceGroup;

  public ResourceGroupDeleteEvent(String accountIdentifier, ResourceGroupDTO resourceGroup) {
    this.accountIdentifier = accountIdentifier;
    this.resourceGroup = resourceGroup;
  }

  @Override
  public ResourceScope getResourceScope() {
    if (isEmpty(resourceGroup.getOrgIdentifier())) {
      return new AccountScope(accountIdentifier);
    } else if (isEmpty(resourceGroup.getProjectIdentifier())) {
      return new OrgScope(accountIdentifier, resourceGroup.getOrgIdentifier());
    }
    return new ProjectScope(accountIdentifier, resourceGroup.getOrgIdentifier(), resourceGroup.getProjectIdentifier());
  }

  @Override
  public Resource getResource() {
    return Resource.builder().identifier(resourceGroup.getIdentifier()).type(RESOURCE_GROUP).build();
  }

  @Override
  public String getEventType() {
    return "ResourceGroupDeleted";
  }
}
