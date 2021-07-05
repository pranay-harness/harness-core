package io.harness.batch.processing.ccm;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AzureStorageSyncRecord {
  // TODO: Update the fields here as needed.
  String accountId;
  String settingId;
  String directoryName;
  String containerName;
  String storageAccountName;
  String subscriptionId;
  String tenantId;
  String reportName;
}