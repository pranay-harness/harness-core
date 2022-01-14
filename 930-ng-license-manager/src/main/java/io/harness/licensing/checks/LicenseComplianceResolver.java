package io.harness.licensing.checks;

import io.harness.ModuleType;
import io.harness.licensing.Edition;
import io.harness.licensing.EditionAction;
import io.harness.licensing.entities.modules.ModuleLicense;

import java.util.Map;
import java.util.Set;

public interface LicenseComplianceResolver {
  void preCheck(ModuleLicense moduleLicense, EditionAction targetAction);
  Map<Edition, Set<EditionAction>> getEditionStates(ModuleType moduleType, String accountId);
}
