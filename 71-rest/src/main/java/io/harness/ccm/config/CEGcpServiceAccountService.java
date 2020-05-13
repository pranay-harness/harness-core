package io.harness.ccm.config;

import java.io.IOException;

public interface CEGcpServiceAccountService {
  String create(String accountId);
  GcpServiceAccount getDefaultServiceAccount(String accountId) throws IOException;
  GcpServiceAccount getByAccountId(String accountId);
}
