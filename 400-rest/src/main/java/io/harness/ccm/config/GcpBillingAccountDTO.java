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
public class GcpBillingAccountDTO {
  String uuid;
  String accountId;
  String organizationSettingId;
  String gcpBillingAccountId;
  String gcpBillingAccountName;
  boolean exportEnabled;
  String bqProjectId;
  String bqDatasetId;
}
