package io.harness.ccm.config;

import software.wings.beans.ValidationResult;

import java.util.List;

public interface GcpOrganizationService {
  ValidationResult validate(GcpOrganization organization);
  GcpOrganization upsert(GcpOrganization organization);
  GcpOrganization get(String uuid);
  List<GcpOrganization> list(String accountId);
}