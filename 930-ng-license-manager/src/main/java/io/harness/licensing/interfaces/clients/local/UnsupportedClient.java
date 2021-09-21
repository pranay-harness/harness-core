package io.harness.licensing.interfaces.clients.local;

import io.harness.exception.UnsupportedOperationException;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.stats.RuntimeUsageDTO;
import io.harness.licensing.interfaces.clients.ModuleLicenseClient;

public class UnsupportedClient implements ModuleLicenseClient {
  @Override
  public ModuleLicenseDTO createTrialLicense(Edition edition, String accountId) {
    throw new UnsupportedOperationException("Requested module type hasn't been supported");
  }

  @Override
  public RuntimeUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}
