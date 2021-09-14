/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.ValidationResult;

import java.util.List;

@OwnedBy(CE)
public interface GcpOrganizationService {
  ValidationResult validate(GcpOrganization organization);
  GcpOrganization upsert(GcpOrganization organization);
  GcpOrganization get(String uuid);
  List<GcpOrganization> list(String accountId);
  boolean delete(String accountId, String uuid);
}
