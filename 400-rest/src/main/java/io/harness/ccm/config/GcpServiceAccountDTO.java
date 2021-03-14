package io.harness.ccm.config;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

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
@TargetModule(Module._490_CE_COMMONS)
public class GcpServiceAccountDTO {
  String serviceAccountId;
  String accountId;
  String gcpUniqueId;
  String email;
}
