package io.harness.ccm.config;

public interface CEGcpServiceAccountService {
  String create(String accountId);
  GcpServiceAccount getDefaultServiceAccount(String accountId);
}
