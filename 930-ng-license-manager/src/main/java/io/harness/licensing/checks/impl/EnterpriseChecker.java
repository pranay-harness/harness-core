/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.licensing.checks.impl;

import io.harness.ModuleType;
import io.harness.licensing.Edition;
import io.harness.licensing.EditionAction;
import io.harness.licensing.checks.LicenseEditionChecker;
import io.harness.licensing.checks.ModuleLicenseState;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.helpers.ModuleLicenseHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnterpriseChecker implements LicenseEditionChecker {
  @Override
  public void preCheck(ModuleLicense moduleLicense, List<ModuleLicense> currentLicenses, EditionAction targetAction) {
    // Enterprise specific check
  }

  @Override
  public Set<EditionAction> getEditionActions(ModuleLicenseState currentModuleState, ModuleType moduleType,
      Map<ModuleType, ModuleLicense> lastExpiredActiveLicenseMap) {
    Set<EditionAction> result = new HashSet<>();

    boolean paidEditionInOtherModules =
        ModuleLicenseHelper.isPaidEditionInOtherModules(moduleType, Edition.TEAM, lastExpiredActiveLicenseMap);
    if (paidEditionInOtherModules) {
      result.add(EditionAction.DISABLED_BY_TEAM);
      return result;
    }

    switch (currentModuleState) {
      case NO_LICENSE:
        result.add(EditionAction.START_TRIAL);
        result.add(EditionAction.CONTACT_SALES);
        break;
      case ACTIVE_TEAM_TRIAL:
      case EXPIRED_TEAM_TRIAL_CAN_EXTEND:
      case EXPIRED_TEAM_TRIAL:
      case ACTIVE_ENTERPRISE_TRIAL:
      case EXPIRED_ENTERPRISE_TRIAL:
        result.add(EditionAction.CONTACT_SALES);
        break;
      case ACTIVE_FREE:
      case ACTIVE_TEAM_PAID:
      case EXPIRED_TEAM_PAID:
        result.add(EditionAction.UPGRADE);
        result.add(EditionAction.CONTACT_SALES);
        break;
      case EXPIRED_ENTERPRISE_TRIAL_CAN_EXTEND:
        result.add(EditionAction.EXTEND_TRIAL);
        result.add(EditionAction.CONTACT_SALES);
        break;
      case ACTIVE_ENTERPRISE_PAID:
      case EXPIRED_ENTERPRISE_PAID:
        result.add(EditionAction.MANAGE);
        break;
      default:
        break;
    }
    return result;
  }
}
