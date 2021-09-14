/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.bugsnag;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.BugsnagConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.ThirdPartyApiCallLog;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

public interface BugsnagDelegateService {
  @DelegateTaskType(TaskType.BUGSNAG_GET_APP_TASK)
  Set<BugsnagApplication> getOrganizations(
      BugsnagConfig config, List<EncryptedDataDetail> encryptedDataDetails, ThirdPartyApiCallLog apiCallLog);
  @DelegateTaskType(TaskType.BUGSNAG_GET_APP_TASK)
  Set<BugsnagApplication> getProjects(BugsnagConfig config, String orgId,
      List<EncryptedDataDetail> encryptedDataDetails, ThirdPartyApiCallLog apiCallLog);
  @DelegateTaskType(TaskType.BUGSNAG_GET_RECORDS)
  Object search(@NotNull BugsnagConfig config, String accountId, BugsnagSetupTestData bugsnagSetupTestData,
      List<EncryptedDataDetail> encryptedDataDetails, ThirdPartyApiCallLog apiCallLog) throws IOException;
}
