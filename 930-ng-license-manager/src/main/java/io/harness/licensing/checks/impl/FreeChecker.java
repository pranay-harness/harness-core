package io.harness.licensing.checks.impl;

import io.harness.ModuleType;
import io.harness.licensing.EditionAction;
import io.harness.licensing.checks.LicenseEditionChecker;
import io.harness.licensing.checks.ModuleLicenseState;
import io.harness.licensing.entities.modules.ModuleLicense;

import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class FreeChecker implements LicenseEditionChecker {
  @Override
  public void preCheck(ModuleLicense moduleLicense, List<ModuleLicense> currentLicenses, EditionAction targetAction) {
    // Free specific check
  }

  @Override
  public Set<EditionAction> getEditionActions(ModuleLicenseState currentModuleState, ModuleType moduleType,
      Map<ModuleType, ModuleLicense> lastExpiredActiveLicenseMap) {
    Set<EditionAction> result = new HashSet<>();

    if (ModuleLicenseState.NO_LICENSE.equals(currentModuleState)) {
      result.add(EditionAction.START_FREE);
    }
    return result;
  }
}
