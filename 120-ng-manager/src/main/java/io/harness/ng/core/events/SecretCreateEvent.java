package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.SECRET;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.secrets.SecretDTOV2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class SecretCreateEvent implements Event {
  private SecretDTOV2 secret;
  private String accountIdentifier;

  public SecretCreateEvent(String accountIdentifier, SecretDTOV2 secret) {
    this.secret = secret;
    this.accountIdentifier = accountIdentifier;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (isNotEmpty(secret.getOrgIdentifier())) {
      if (isEmpty(secret.getProjectIdentifier())) {
        return new OrgScope(accountIdentifier, secret.getOrgIdentifier());
      } else {
        return new ProjectScope(accountIdentifier, secret.getOrgIdentifier(), secret.getProjectIdentifier());
      }
    }
    return new AccountScope(accountIdentifier);
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, secret.getName());
    return Resource.builder().identifier(secret.getIdentifier()).type(SECRET).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return "SecretCreated";
  }
}
