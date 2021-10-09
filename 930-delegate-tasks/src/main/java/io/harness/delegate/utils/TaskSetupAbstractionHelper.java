package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(PL)
public class TaskSetupAbstractionHelper {
  private static final String PROJECT_OWNER = "%s/%s";
  private static final String ORG_OWNER = "%s";

  public String getOwner(String accountId, String orgIdentifier, String projectIdentifier) {
    String owner = null;
    if (isNotEmpty(orgIdentifier) && isNotEmpty(projectIdentifier)) {
      owner = String.format(PROJECT_OWNER, orgIdentifier, projectIdentifier);
    } else if (isNotEmpty(orgIdentifier)) {
      owner = String.format(ORG_OWNER, orgIdentifier);
    }
    return owner;
  }
}
