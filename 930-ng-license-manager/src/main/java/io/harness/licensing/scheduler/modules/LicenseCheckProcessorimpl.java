package io.harness.licensing.scheduler.modules;

import static io.harness.licensing.interfaces.ModuleLicenseInterfaceImpl.TRIAL_DURATION;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.entities.modules.CFModuleLicense;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.licensing.scheduler.LicenseCheckProcessor;
import io.harness.telemetry.Category;
import io.harness.telemetry.Destination;
import io.harness.telemetry.TelemetryReporter;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class LicenseCheckProcessorimpl implements LicenseCheckProcessor {
  private final TelemetryReporter telemetryReporter;
  private static final String TRIAL_ENDED = "TRIAL_ENDED";

  @Override
  public CheckResult checkExpiry(
      String uuid, long currentTime, ModuleLicense moduleLicense, TransactionAggregator transactionAggregate) {
    if (Edition.FREE.equals(moduleLicense.getEdition())) {
      // free edition module license is always active and skip subscription check
      return CheckResult.builder().allInactive(false).isUpdated(false).build();
    }

    // Transaction check
    CheckResult checkResult = CheckResult.newDefaultResult();
    long maxExpiryTime = 0;
    for (LicenseTransaction transaction : moduleLicense.getTransactions()) {
      // transaction expiry check
      if (transaction.checkExpiry(currentTime)) {
        if (transaction.isActive()) {
          // Expire transaction if still active
          transaction.setStatus(LicenseStatus.EXPIRED);
          checkResult.setUpdated(true);

          if (LicenseType.TRIAL.equals(transaction.getLicenseType())) {
            sendTrialEndEvents(transaction, moduleLicense.getCreatedBy().getEmail());
          }
        }
      } else {
        // In case Active transaction
        checkResult.setAllInactive(false);
        maxExpiryTime = maxExpiryTime >= transaction.getExpiryTime() ? maxExpiryTime : transaction.getExpiryTime();
        transactionAggregate.processTransaction(checkResult, transaction);
      }
    }

    checkResult.setMaxExpiryTime(maxExpiryTime);
    compareModuleLicenseAndCheckResult(checkResult, moduleLicense);
    return checkResult;
  }

  private boolean verifyIfStatusNotMatch(boolean transactionAllInactive, boolean licenseIsInActive) {
    return transactionAllInactive == licenseIsInActive;
  }

  private void compareModuleLicenseAndCheckResult(CheckResult checkResult, ModuleLicense moduleLicense) {
    // module specific data compare
    switch (moduleLicense.getModuleType()) {
      case CD:
        CDModuleLicense cdModuleLicense = (CDModuleLicense) moduleLicense;
        if (cdModuleLicense.getTotalWorkLoads() != checkResult.getTotalWorkload()) {
          cdModuleLicense.setTotalWorkLoads(checkResult.getTotalWorkload());
          checkResult.setUpdated(true);
        }
        break;
      case CI:
        CIModuleLicense ciModuleLicense = (CIModuleLicense) moduleLicense;
        if (ciModuleLicense.getTotalDevelopers() != checkResult.getTotalDevelopers()) {
          ciModuleLicense.setTotalDevelopers(checkResult.getTotalDevelopers());
          checkResult.setUpdated(true);
        }
        break;
      case CE:
        break;
      case CF:
        CFModuleLicense cfModuleLicense = (CFModuleLicense) moduleLicense;
        if (cfModuleLicense.getTotalClientMAUs() != checkResult.getTotalClientMAU()
            || cfModuleLicense.getTotalFeatureFlagUnit() != checkResult.getTotalFeatureFlagUnit()) {
          cfModuleLicense.setTotalFeatureFlagUnit(checkResult.getTotalFeatureFlagUnit());
          cfModuleLicense.setTotalClientMAUs(checkResult.getTotalClientMAU());
          checkResult.setUpdated(true);
        }
        break;
      case CV:
        break;
      default:
        log.error("Unknown Module license type");
    }
    // check max expiry time
    if (moduleLicense.getExpireTime() < checkResult.getMaxExpiryTime()) {
      moduleLicense.setExpireTime(checkResult.getMaxExpiryTime());
      checkResult.setUpdated(true);
    }

    // check module license status
    if (verifyIfStatusNotMatch(checkResult.isAllInactive(), moduleLicense.isActive())) {
      moduleLicense.setStatus(checkResult.isAllInactive() ? LicenseStatus.EXPIRED : LicenseStatus.ACTIVE);
      checkResult.setUpdated(true);
    }
  }

  private void sendTrialEndEvents(LicenseTransaction licenseTransaction, String email) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("email", email);
    properties.put("module", licenseTransaction.getModuleType());
    properties.put("licenseType", licenseTransaction.getLicenseType());
    properties.put("plan", licenseTransaction.getEdition());
    properties.put("platform", "NG");
    properties.put("startTime", String.valueOf(licenseTransaction.getStartTime()));
    properties.put("endTime", String.valueOf(Instant.now().toEpochMilli()));
    properties.put("duration", TRIAL_DURATION);
    telemetryReporter.sendTrackEvent(TRIAL_ENDED, email, licenseTransaction.getAccountIdentifier(), properties,
        ImmutableMap.<Destination, Boolean>builder().put(Destination.MARKETO, true).build(), Category.SIGN_UP);
  }
}
