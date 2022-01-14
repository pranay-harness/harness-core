package io.harness.resourcegroup.framework.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.RESOURCE_GROUP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class ResourceGroupUpdateEvent implements Event {
  String accountIdentifier;
  ResourceGroupDTO newResourceGroup;
  ResourceGroupDTO oldResourceGroup;

  public ResourceGroupUpdateEvent(
      String accountIdentifier, ResourceGroupDTO newResourceGroup, ResourceGroupDTO oldResourceGroup) {
    this.accountIdentifier = accountIdentifier;
    this.newResourceGroup = newResourceGroup;
    this.oldResourceGroup = oldResourceGroup;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (isEmpty(newResourceGroup.getOrgIdentifier())) {
      return new AccountScope(accountIdentifier);
    } else if (isEmpty(newResourceGroup.getProjectIdentifier())) {
      return new OrgScope(accountIdentifier, newResourceGroup.getOrgIdentifier());
    }
    return new ProjectScope(
        accountIdentifier, newResourceGroup.getOrgIdentifier(), newResourceGroup.getProjectIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, newResourceGroup.getName());
    return Resource.builder().identifier(newResourceGroup.getIdentifier()).type(RESOURCE_GROUP).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "ResourceGroupUpdated";
  }
}
