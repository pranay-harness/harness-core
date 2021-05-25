package io.harness.licensing.scheduler;

import io.harness.licensing.entities.modules.ModuleLicense;

public interface LicenseCheckProcessor {
  CheckResult checkExpiry(String uuid, long currentTime, ModuleLicense moduleLicense);
}
