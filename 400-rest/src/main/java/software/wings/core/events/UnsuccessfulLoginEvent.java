package software.wings.core.events;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceTypeConstants;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.PL)
@Getter
@NoArgsConstructor
public class UnsuccessfulLoginEvent implements Event {
  public static final String UNSUCCESSFUL_LOGIN = "UnsuccessfulLogin";
  private String accountIdentifier;
  private String userId;
  private String email;
  private String userName;

  public UnsuccessfulLoginEvent(String accountIdentifier, String userId, String email, String userName) {
    this.accountIdentifier = accountIdentifier;
    this.userId = userId;
    this.email = email;
    this.userName = userName;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return new AccountScope(accountIdentifier);
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    if (isNotEmpty(userName)) {
      labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, userName);
    }
    labels.put(ResourceConstants.LABEL_KEY_USER_ID, userId);
    return Resource.builder().identifier(email).type(ResourceTypeConstants.USER).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return UNSUCCESSFUL_LOGIN;
  }
}
