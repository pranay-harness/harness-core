package io.harness.overviewdashboard.rbac.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.overviewdashboard.rbac.service.DashboardRBACService;
import io.harness.remote.client.RestClientUtils;
import io.harness.user.remote.UserClient;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(PL)
public class DashboardRBACServiceImpl implements DashboardRBACService {
  @Inject private UserClient userClient;

  @Override
  public List<ProjectDTO> listAccessibleProject(String accountId, String userId) {
    return RestClientUtils.getResponse(userClient.getUserAllProjectsInfo(accountId, userId));
  }
}
