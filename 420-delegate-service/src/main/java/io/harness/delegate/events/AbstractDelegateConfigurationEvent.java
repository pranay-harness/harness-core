package io.harness.delegate.events;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.ResourceScope;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@NoArgsConstructor
public abstract class AbstractDelegateConfigurationEvent implements Event {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;

  @Override
  public ResourceScope getResourceScope() {
    Preconditions.checkNotNull(accountIdentifier);
    if (isNotEmpty(projectIdentifier) && isNotEmpty(orgIdentifier)) {
      return new ProjectScope(accountIdentifier, orgIdentifier, projectIdentifier);
    }

    if (isEmpty(projectIdentifier) && isNotEmpty(orgIdentifier)) {
      return new OrgScope(accountIdentifier, orgIdentifier);
    }

    return new AccountScope(accountIdentifier);
  }
}
