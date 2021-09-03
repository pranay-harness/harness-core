package io.harness.feature.helpers.impl;

import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.ModuleType;
import io.harness.feature.helpers.LicenseHelper;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.licensing.remote.NgLicenseHttpClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CentralizedLicenseHelper implements LicenseHelper {
  @Inject NgLicenseHttpClient ngLicenseHttpClient;

  @Override
  public Optional<LicensesWithSummaryDTO> getLicenseSummary(String accountIdentifier, ModuleType moduleType) {
    if (ModuleType.CORE.equals(moduleType)) {
      // Need to conclude valid license with highest edition under the account
      return Optional.ofNullable(electHighestEditionSummary(accountIdentifier));
    } else {
      return Optional.ofNullable(
          getResponse(ngLicenseHttpClient.getLicenseSummary(accountIdentifier, moduleType.name())));
    }
  }

  private LicensesWithSummaryDTO electHighestEditionSummary(String accountIdentifier) {
    LicensesWithSummaryDTO licensesWithSummaryDTO = null;
    long currentTime = Instant.now().toEpochMilli();
    for (ModuleType moduleType : ModuleType.values()) {
      // Internal module type is not allowed
      if (moduleType.isInternal()) {
        continue;
      }

      try {
        LicensesWithSummaryDTO response =
            getResponse(ngLicenseHttpClient.getLicenseSummary(accountIdentifier, moduleType.name()));
        // Expired license doesn't take into account
        if (isExpired(response, currentTime)) {
          continue;
        }

        if (isHigherEdition(licensesWithSummaryDTO, response)) {
          licensesWithSummaryDTO = response;
        }
      } catch (Exception e) {
        log.error(
            String.format("Failed to query [%s] license for account [%s]", moduleType.name(), accountIdentifier), e);
      }
    }
    return licensesWithSummaryDTO;
  }

  private boolean isExpired(LicensesWithSummaryDTO summaryDTO, long currentTime) {
    return summaryDTO == null || summaryDTO.getMaxExpiryTime() <= currentTime;
  }

  private boolean isHigherEdition(LicensesWithSummaryDTO current, LicensesWithSummaryDTO compare) {
    return current == null || current.getEdition().compareTo(compare.getEdition()) < 0;
  }
}
