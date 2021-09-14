/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum AuthorizationServiceHeader {
  ANALYZER_SERVICE("AnalyzerService"),
  BEARER("Bearer"),
  MANAGER("Manager"),
  NG_MANAGER("NextGenManager"),
  BATCH_PROCESSING("BatchProcessing"),
  CI_MANAGER("CIManager"),
  CV_NEXT_GEN("CVNextGen"),
  CE_NEXT_GEN("CENextGen"),
  DELEGATE_SERVICE("DelegateService"),
  IDENTITY_SERVICE("IdentityService"),
  ADMIN_PORTAL("AdminPortal"),
  NOTIFICATION_SERVICE("NotificationService"),
  AUDIT_SERVICE("AuditService"),
  PIPELINE_SERVICE("PipelineService"),
  TEMPLATE_SERVICE("TemplateService"),
  ACCESS_CONTROL_SERVICE("accessControlService"),
  RESOUCE_GROUP_SERVICE("ResourceGroupService"),
  PLATFORM_SERVICE("PlatformService"),
  DEFAULT("Default");

  private final String serviceId;

  AuthorizationServiceHeader(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getServiceId() {
    return serviceId;
  }
}
