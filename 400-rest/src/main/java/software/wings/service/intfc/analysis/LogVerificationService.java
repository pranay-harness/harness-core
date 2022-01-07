/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.analysis;

import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.bugsnag.BugsnagApplication;
import software.wings.service.impl.bugsnag.BugsnagSetupTestData;
import software.wings.sm.StateType;

import java.util.Set;
import javax.validation.constraints.NotNull;

public interface LogVerificationService {
  Set<BugsnagApplication> getOrgProjectListBugsnag(
      @NotNull String settingId, @NotNull String orgId, @NotNull StateType stateType, boolean shouldGetProjects);
  VerificationNodeDataSetupResponse getTestLogData(String accountId, BugsnagSetupTestData bugsnagSetupTestData);
}
