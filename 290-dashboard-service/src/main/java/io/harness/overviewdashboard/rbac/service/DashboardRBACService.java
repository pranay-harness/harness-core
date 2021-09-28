package io.harness.overviewdashboard.rbac.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ProjectDTO;

import java.util.List;

@OwnedBy(PL)
public interface DashboardRBACService {
  List<ProjectDTO> listAccessibleProject(String accountIdentifier, String userId);
}
