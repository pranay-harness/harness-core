package io.harness.licensing.scheduler.modules;

import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.licensing.scheduler.LicenseCheckProcessor;

import com.google.inject.Singleton;

@Singleton
public class CVCheckProcessor implements LicenseCheckProcessor {
  @Override
  public CheckResult checkExpiry(String uuid, long currentTime, ModuleLicense moduleLicense) {
    // TODO implement CVCHeckProcessor when CVModuleLicense is ready
    return CheckResult.builder().allInactive(true).isUpdated(false).build();
  }
}
