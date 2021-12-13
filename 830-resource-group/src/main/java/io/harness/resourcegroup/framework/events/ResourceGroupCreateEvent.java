package io.harness.resourcegroup.framework.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceType;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class ResourceGroupCreateEvent implements Event {
  String accountIdentifier;
  ResourceGroupDTO resourceGroup;

  public ResourceGroupCreateEvent(String accountIdentifier, ResourceGroupDTO resourceGroup) {
    this.accountIdentifier = accountIdentifier;
    this.resourceGroup = resourceGroup;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (isEmpty(resourceGroup.getOrgIdentifier())) {
      return new AccountScope(accountIdentifier);
    } else if (isEmpty(resourceGroup.getProjectIdentifier())) {
      return new OrgScope(accountIdentifier, resourceGroup.getOrgIdentifier());
    }
    return new ProjectScope(accountIdentifier, resourceGroup.getOrgIdentifier(), resourceGroup.getProjectIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder()
        .identifier(resourceGroup.getIdentifier())
        .type(ResourceType.RESOURCE_GROUP.name())
        .build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "ResourceGroupCreated";
  }
}
