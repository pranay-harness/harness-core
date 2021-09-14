/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.GCP_RESOURCE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CE)
public class GcpOrganizationDTO {
  String uuid;
  String accountId;
  String organizationId;
  String organizationName;
  String serviceAccount;
}
