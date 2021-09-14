/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.serviceAccount;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.google.api.services.iam.v1.model.ServiceAccount;
import java.io.IOException;

@OwnedBy(CE)
public interface GcpServiceAccountService {
  ServiceAccount create(String serviceAccountId, String displayName, String ccmProjectId) throws IOException;
  void setIamPolicies(String serviceAccountEmail) throws IOException;
  void addRolesToServiceAccount(String serviceAccountEmail, String[] roles, String ccmProjectId);
}
