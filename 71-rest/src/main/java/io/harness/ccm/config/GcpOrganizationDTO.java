package io.harness.ccm.config;

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
public class GcpOrganizationDTO {
  String uuid;
  String accountId;
  String organizationId;
  String organizationName;
  String serviceAccount;
}
