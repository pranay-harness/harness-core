/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.audit.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditSettingsDTO;
import io.harness.audit.entities.AuditSettings;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AuditSettingsMapper {
  public static AuditSettings fromDTO(AuditSettingsDTO auditSettingsDTO) {
    return AuditSettings.builder().retentionPeriodInMonths(auditSettingsDTO.getRetentionPeriodInMonths()).build();
  }

  public static AuditSettingsDTO toDTO(AuditSettings auditSettings) {
    return AuditSettingsDTO.builder().retentionPeriodInMonths(auditSettings.getRetentionPeriodInMonths()).build();
  }
}
