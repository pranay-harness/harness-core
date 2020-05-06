package io.harness.ccm.config;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.GCP_RESOURCE)
@FieldDefaults(level = AccessLevel.PRIVATE)
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
