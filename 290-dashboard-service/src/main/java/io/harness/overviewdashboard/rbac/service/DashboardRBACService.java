/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.overviewdashboard.rbac.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ProjectDTO;

import java.util.List;

@OwnedBy(PL)
public interface DashboardRBACService {
  List<ProjectDTO> listAccessibleProject(String accountId, String userId);
}
