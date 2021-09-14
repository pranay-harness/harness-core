/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.audit.retention;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.AuditService;
import io.harness.audit.api.AuditSettingsService;
import io.harness.audit.entities.AuditSettings;

import com.google.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class AuditAccountSyncJob implements Runnable {
  @Inject private AuditService auditService;
  @Inject private AuditSettingsService auditSettingsService;

  @Override
  public void run() {
    try {
      Set<String> accountIdentifiers = auditService.getUniqueAuditedAccounts();
      Set<String> accountsInDBWithRetentionPeriod =
          auditSettingsService.fetchAll().stream().map(AuditSettings::getAccountIdentifier).collect(Collectors.toSet());

      accountIdentifiers.removeAll(accountsInDBWithRetentionPeriod);
      accountIdentifiers.forEach(accountIdentifier -> auditSettingsService.create(accountIdentifier, 24));
    } catch (Exception e) {
      log.error("Error in Account and audit retention sync job", e);
    }
  }
}
