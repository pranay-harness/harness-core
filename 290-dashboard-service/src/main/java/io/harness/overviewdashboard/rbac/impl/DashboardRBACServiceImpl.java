package io.harness.overviewdashboard.rbac.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.overviewdashboard.rbac.service.DashboardRBACService;
import io.harness.remote.client.RestClientUtils;
import io.harness.user.remote.UserClient;

import com.google.inject.Inject;
import io.harness.userng.remote.UserNGClient;

import java.util.List;

@OwnedBy(PL)
public class DashboardRBACServiceImpl implements DashboardRBACService {
  @Inject private UserNGClient userNGClient;

  @Override
  public List<ProjectDTO> listAccessibleProject(String accountId, String userId) {
    return RestClientUtils.getResponse(userNGClient.getUserAllProjectsInfo(accountId, userId));
  }
}
