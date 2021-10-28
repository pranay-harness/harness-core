package io.harness.licensing.checks.impl;

import static io.harness.configuration.DeployMode.DEPLOY_MODE;
import static io.harness.configuration.DeployMode.isOnPrem;

import io.harness.ModuleType;
import io.harness.exception.InvalidRequestException;
import io.harness.licensing.Edition;
import io.harness.licensing.EditionAction;
import io.harness.licensing.checks.LicenseComplianceResolver;
import io.harness.licensing.checks.LicenseEditionChecker;
import io.harness.licensing.checks.ModuleLicenseState;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.helpers.ModuleLicenseHelper;
import io.harness.repositories.ModuleLicenseRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class DefaultLicenseComplianceResolver implements LicenseComplianceResolver {
  private final ModuleLicenseRepository licenseRepository;
  private final Map<Edition, LicenseEditionChecker> licenseEditionCheckerMap;

  @Inject
  public DefaultLicenseComplianceResolver(
      ModuleLicenseRepository licenseRepository, Map<Edition, LicenseEditionChecker> licenseEditionCheckerMap) {
    this.licenseRepository = licenseRepository;
    this.licenseEditionCheckerMap = licenseEditionCheckerMap;
  }

  @Override
  public void preCheck(ModuleLicense moduleLicense, EditionAction targetAction) {
    ModuleType moduleType = moduleLicense.getModuleType();
    String accountId = moduleLicense.getAccountIdentifier();

    List<ModuleLicense> allLicenses = getAllLicensesUnderAccount(accountId);
    Map<ModuleType, ModuleLicense> lastExpiredLicenseMap =
        ModuleLicenseHelper.getLastExpiredLicenseForEachModuleType(allLicenses);

    List<ModuleLicense> currentLicenses = licenseRepository.findByAccountIdentifierAndModuleType(accountId, moduleType);
    ModuleLicenseState currentModuleState = ModuleLicenseHelper.getCurrentModuleState(currentLicenses);

    // License state and action check
    Set<EditionAction> editionActions = licenseEditionCheckerMap.get(moduleLicense.getEdition())
                                            .getEditionActions(currentModuleState, moduleType, lastExpiredLicenseMap);
    if (!editionActions.contains(targetAction)) {
      throw new InvalidRequestException(
          String.format("[%s] is not allowed, current license state [%s]", targetAction.name(), currentModuleState));
    }

    // Edition specific check
    licenseEditionCheckerMap.get(moduleLicense.getEdition()).preCheck(moduleLicense, currentLicenses, targetAction);
  }

  @Override
  public Map<Edition, Set<EditionAction>> getEditionStates(ModuleType moduleType, String accountId) {
    List<ModuleLicense> allLicenses = getAllLicensesUnderAccount(accountId);
    Map<ModuleType, ModuleLicense> lastExpiredLicenseMap =
        ModuleLicenseHelper.getLastExpiredLicenseForEachModuleType(allLicenses);

    List<ModuleLicense> currentLicenses = licenseRepository.findByAccountIdentifierAndModuleType(accountId, moduleType);
    ModuleLicenseState currentModuleState = ModuleLicenseHelper.getCurrentModuleState(currentLicenses);
    Map<Edition, Set<EditionAction>> result = new HashMap<>();
    // Current edition state only support SAAS
    if (!isOnPrem(System.getenv().get(DEPLOY_MODE))) {
      for (Edition edition : Edition.getSaasEditions()) {
        Set<EditionAction> editionActions = licenseEditionCheckerMap.get(edition).getEditionActions(
            currentModuleState, moduleType, lastExpiredLicenseMap);
        result.put(edition, editionActions);
      }
    }
    return result;
  }

  private List<ModuleLicense> getAllLicensesUnderAccount(String accountId) {
    return licenseRepository.findByAccountIdentifier(accountId);
  }
}
