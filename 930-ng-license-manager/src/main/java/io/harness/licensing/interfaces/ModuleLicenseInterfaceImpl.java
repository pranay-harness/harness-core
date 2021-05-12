package io.harness.licensing.interfaces;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.stats.RuntimeUsageDTO;
import io.harness.licensing.interfaces.clients.ModuleLicenseClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@OwnedBy(HarnessTeam.GTM)
@Singleton
public class ModuleLicenseInterfaceImpl implements ModuleLicenseInterface {
  @Inject Map<ModuleType, ModuleLicenseClient> clientMap;
  static final long TRIAL_DURATION = 14;

  @Override
  public ModuleLicenseDTO generateTrialLicense(
      Edition edition, String accountId, LicenseType licenseType, ModuleType moduleType) {
    ModuleLicenseDTO trialLicense = clientMap.get(moduleType).createTrialLicense(edition, accountId, licenseType);
    trialLicense.setAccountIdentifier(accountId);
    checkAndSetDefault(trialLicense, moduleType, licenseType, edition);
    return trialLicense;
  }

  @Override
  public RuntimeUsageDTO getRuntimeUsage(String accountId, ModuleType moduleType) {
    throw new UnsupportedOperationException("getRuntimeUsage not supported");
  }

  private void checkAndSetDefault(
      ModuleLicenseDTO moduleLicenseDTO, ModuleType moduleType, LicenseType type, Edition edition) {
    if (moduleLicenseDTO.getModuleType() == null) {
      moduleLicenseDTO.setModuleType(moduleType);
    }
    if (moduleLicenseDTO.getEdition() == null) {
      moduleLicenseDTO.setEdition(edition);
    }
    if (moduleLicenseDTO.getLicenseType() == null) {
      moduleLicenseDTO.setLicenseType(type);
    }
    if (moduleLicenseDTO.getStartTime() == 0) {
      moduleLicenseDTO.setStartTime(Instant.now().getEpochSecond());
    }
    if (moduleLicenseDTO.getExpiryTime() == 0) {
      moduleLicenseDTO.setExpiryTime(Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli());
    }
    if (moduleLicenseDTO.getStatus() == null) {
      moduleLicenseDTO.setStatus(LicenseStatus.ACTIVE);
    }
  }
}
