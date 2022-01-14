package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import java.io.IOException;

@OwnedBy(CE)
public interface CEGcpServiceAccountService {
  String create(String accountId);
  GcpServiceAccount getDefaultServiceAccount(String accountId) throws IOException;
  GcpServiceAccount getByAccountId(String accountId);
  GcpServiceAccount getByServiceAccountId(String accountId);
}
