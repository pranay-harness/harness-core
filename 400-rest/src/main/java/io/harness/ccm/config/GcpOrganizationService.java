package io.harness.ccm.config;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.ValidationResult;

import java.util.List;
@TargetModule(Module._490_CE_COMMONS)
public interface GcpOrganizationService {
  ValidationResult validate(GcpOrganization organization);
  GcpOrganization upsert(GcpOrganization organization);
  GcpOrganization get(String uuid);
  List<GcpOrganization> list(String accountId);
  boolean delete(String accountId, String uuid);
}
