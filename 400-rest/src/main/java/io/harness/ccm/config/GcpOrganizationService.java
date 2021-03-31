package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.ValidationResult;

import java.util.List;

@OwnedBy(CE)
public interface GcpOrganizationService {
  ValidationResult validate(GcpOrganization organization);
  GcpOrganization upsert(GcpOrganization organization);
  GcpOrganization get(String uuid);
  List<GcpOrganization> list(String accountId);
  boolean delete(String accountId, String uuid);
}
