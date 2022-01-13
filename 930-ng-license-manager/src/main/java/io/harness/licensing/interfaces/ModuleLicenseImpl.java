/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.licensing.interfaces;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.interfaces.clients.ModuleLicenseClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;

@OwnedBy(HarnessTeam.GTM)
@Singleton
public class ModuleLicenseImpl implements ModuleLicenseInterface {
  @Inject Map<ModuleType, ModuleLicenseClient> clientMap;
  public static final long TRIAL_DURATION = 14;

  @Override
  public ModuleLicenseDTO generateFreeLicense(String accountId, ModuleType moduleType) {
    ModuleLicenseDTO trialLicense = clientMap.get(moduleType).createTrialLicense(Edition.FREE, accountId);
    trialLicense.setAccountIdentifier(accountId);
    checkAndSetDefault(trialLicense, moduleType, Edition.FREE);
    return trialLicense;
  }

  @Override
  public ModuleLicenseDTO generateCommunityLicense(String accountId, ModuleType moduleType) {
    ModuleLicenseDTO trialLicense = clientMap.get(moduleType).createTrialLicense(Edition.COMMUNITY, accountId);
    trialLicense.setAccountIdentifier(accountId);
    checkAndSetDefault(trialLicense, moduleType, Edition.COMMUNITY);
    return trialLicense;
  }

  @Override
  public ModuleLicenseDTO generateTrialLicense(Edition edition, String accountId, ModuleType moduleType) {
    ModuleLicenseDTO trialLicense = clientMap.get(moduleType).createTrialLicense(edition, accountId);
    trialLicense.setAccountIdentifier(accountId);
    checkAndSetDefault(trialLicense, moduleType, edition);
    return trialLicense;
  }

  private void checkAndSetDefault(ModuleLicenseDTO moduleLicenseDTO, ModuleType moduleType, Edition edition) {
    if (moduleLicenseDTO.getModuleType() == null) {
      moduleLicenseDTO.setModuleType(moduleType);
    }
    if (moduleLicenseDTO.getEdition() == null) {
      moduleLicenseDTO.setEdition(edition);
    }
    if (moduleLicenseDTO.getStatus() == null) {
      moduleLicenseDTO.setStatus(LicenseStatus.ACTIVE);
    }
  }
}
