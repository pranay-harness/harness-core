/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.serviceAccount;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.google.api.services.cloudresourcemanager.model.Policy;

@OwnedBy(CE)
public interface GcpResourceManagerService {
  void setPolicy(String projectId, Policy policy);
  Policy getIamPolicy(String projectId);
}
