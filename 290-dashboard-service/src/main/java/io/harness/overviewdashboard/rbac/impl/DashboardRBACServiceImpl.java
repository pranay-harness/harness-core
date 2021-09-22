package io.harness.overviewdashboard.rbac.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.overviewdashboard.rbac.service.DashboardRBACService;
import io.harness.userng.remote.UserNGClient;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(PL)
public class DashboardRBACServiceImpl implements DashboardRBACService {
  @Inject private UserNGClient userNGClient;

  @Override
  public List<ProjectDTO> listAccessibleProject(String accountIdentifier, String userId) {
    return getResponse(userNGClient.getUserAllProjectsInfo(accountIdentifier, userId));
  }
}
