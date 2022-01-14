package io.harness.licensing.interfaces.clients;

import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;

public interface CDModuleLicenseClient extends ModuleLicenseClient<CDModuleLicenseDTO> {
  @Override CDModuleLicenseDTO createTrialLicense(Edition edition, String accountId);
}
