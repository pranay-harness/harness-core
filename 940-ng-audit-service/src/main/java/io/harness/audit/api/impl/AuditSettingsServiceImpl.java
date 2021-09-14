/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.audit.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.AuditSettingsService;
import io.harness.audit.entities.AuditSettings;
import io.harness.audit.repositories.AuditRetentionRepository;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@OwnedBy(PL)
public class AuditSettingsServiceImpl implements AuditSettingsService {
  @Inject private AuditRetentionRepository auditRetentionRepository;

  @Override
  public AuditSettings getAuditRetentionPolicy(String accountIdentifier) {
    Optional<AuditSettings> optionalAuditRetention =
        auditRetentionRepository.findByAccountIdentifier(accountIdentifier);
    return optionalAuditRetention.orElse(null);
  }

  @Override
  public AuditSettings update(String accountIdentifier, int months) {
    AuditSettings auditSettings = getAuditRetentionPolicy(accountIdentifier);
    if (auditSettings != null) {
      auditSettings.setRetentionPeriodInMonths(months);
      return auditRetentionRepository.save(auditSettings);
    }
    return create(accountIdentifier, months);
  }

  @Override
  public AuditSettings create(String accountIdentifier, int months) {
    AuditSettings auditSettings =
        AuditSettings.builder().accountIdentifier(accountIdentifier).retentionPeriodInMonths(months).build();
    return auditRetentionRepository.save(auditSettings);
  }

  @Override
  public List<AuditSettings> fetchAll() {
    List<AuditSettings> auditSettingsList = new ArrayList<>();
    auditRetentionRepository.findAll().iterator().forEachRemaining(auditSettingsList::add);
    return auditSettingsList;
  }
}
