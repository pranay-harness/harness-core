package io.harness.ng.core.remote.licenserestriction;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.ng.core.services.ProjectService;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PL)
public class ProjectRestrictionsUsageImpl implements RestrictionUsageInterface {
  @Inject private ProjectService projectService;

  @Override
  public long getCurrentValue(String accountIdentifier) {
    return projectService.countProjects(accountIdentifier);
  }
}
