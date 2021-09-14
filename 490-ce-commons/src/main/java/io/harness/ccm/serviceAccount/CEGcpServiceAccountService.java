/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.serviceAccount;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.CEGcpServiceAccount;

import java.io.IOException;

@OwnedBy(CE)
public interface CEGcpServiceAccountService {
  CEGcpServiceAccount create(String accountId, String ccmProjectId);
  CEGcpServiceAccount provisionAndRetrieveServiceAccount(String accountId, String ccmProjectId) throws IOException;
}
