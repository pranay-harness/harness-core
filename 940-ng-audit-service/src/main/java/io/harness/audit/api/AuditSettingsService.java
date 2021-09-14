/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.audit.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.entities.AuditSettings;

import java.util.List;

@OwnedBy(PL)
public interface AuditSettingsService {
  AuditSettings getAuditRetentionPolicy(String accountIdentifier);
  AuditSettings create(String accountIdentifier, int months);
  AuditSettings update(String accountIdentifier, int months);
  List<AuditSettings> fetchAll();
}
