package io.harness.ccm.config;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.io.IOException;
@TargetModule(Module._490_CE_COMMONS)
public interface CEGcpServiceAccountService {
  String create(String accountId);
  GcpServiceAccount getDefaultServiceAccount(String accountId) throws IOException;
  GcpServiceAccount getByAccountId(String accountId);
}
