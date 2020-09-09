package io.harness.marketplace.gcp.procurement.pubsub;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountStatus;
import software.wings.beans.LicenseInfo;
import software.wings.beans.MarketPlace;
import software.wings.licensing.LicenseService;

@Slf4j
@Singleton
public class GcpAccountsHandler {
  @Inject private LicenseService licenseService;

  public void handleAccountDeleteEvent(MarketPlace marketPlace) {
    logger.info("Deleting GCP Provisioned Account with Harness AccountId: {} and GCP AccountId: {}.",
        marketPlace.getAccountId(), marketPlace.getCustomerIdentificationCode());
    licenseService.updateAccountLicense(
        marketPlace.getAccountId(), LicenseInfo.builder().accountStatus(AccountStatus.MARKED_FOR_DELETION).build());
  }
}
