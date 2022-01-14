package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.USER_GROUP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.UserGroupDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class UserGroupCreateEvent implements Event {
  private String accountIdentifier;
  private UserGroupDTO userGroup;

  public UserGroupCreateEvent(String accountIdentifier, UserGroupDTO userGroup) {
    this.accountIdentifier = accountIdentifier;
    this.userGroup = userGroup;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (isEmpty(userGroup.getOrgIdentifier())) {
      return new AccountScope(accountIdentifier);
    } else if (isEmpty(userGroup.getProjectIdentifier())) {
      return new OrgScope(accountIdentifier, userGroup.getOrgIdentifier());
    }
    return new ProjectScope(accountIdentifier, userGroup.getOrgIdentifier(), userGroup.getProjectIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, userGroup.getName());
    return Resource.builder().identifier(userGroup.getIdentifier()).type(USER_GROUP).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "UserGroupCreated";
  }
}
