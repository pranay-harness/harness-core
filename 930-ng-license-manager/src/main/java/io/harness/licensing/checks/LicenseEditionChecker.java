package io.harness.licensing.checks;

import io.harness.ModuleType;
import io.harness.licensing.EditionAction;
import io.harness.licensing.entities.modules.ModuleLicense;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LicenseEditionChecker {
  void preCheck(ModuleLicense moduleLicense, List<ModuleLicense> currentLicenses, EditionAction targetAction);
  Set<EditionAction> getEditionActions(ModuleLicenseState currentModuleState, ModuleType moduleType,
      Map<ModuleType, ModuleLicense> lastExpiredActiveLicenseMap);
}
