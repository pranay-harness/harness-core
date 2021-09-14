/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import java.io.IOException;

@OwnedBy(CE)
public interface CEGcpServiceAccountService {
  String create(String accountId);
  GcpServiceAccount getDefaultServiceAccount(String accountId) throws IOException;
  GcpServiceAccount getByAccountId(String accountId);
}
