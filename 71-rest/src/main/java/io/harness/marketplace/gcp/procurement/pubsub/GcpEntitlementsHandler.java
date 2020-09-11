package io.harness.marketplace.gcp.procurement.pubsub;

import static io.harness.marketplace.gcp.GcpMarketPlaceConstants.DEFAULT_LICENCE_UNITS;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.marketplace.gcp.procurement.GcpProcurementService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.MarketPlace;
import software.wings.beans.marketplace.gcp.GCPBillingJobEntity;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;
import software.wings.service.impl.LicenseUtils;
import software.wings.service.intfc.marketplace.gcp.GCPBillingPollingService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Singleton
public class GcpEntitlementsHandler {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private LicenseService licenseService;
  @Inject private GCPBillingPollingService gcpBillingPollingService;
  @Inject private GcpProcurementService gcpProcurementService;

  public void handleEntitlementCreation(MarketPlace marketPlace, ProcurementPubsubMessage pubsubMessage) {
    gcpProcurementService.approveEntitlement(marketPlace, pubsubMessage.getEntitlement().getId());
  }

  public void handleEntitlementActive(MarketPlace marketPlace) {
    logger.info("Activating Account, GCP AccountId: {}, Harness AccountId: {}",
        marketPlace.getCustomerIdentificationCode(), marketPlace.getAccountId());
    licenseService.updateAccountLicense(marketPlace.getAccountId(),
        LicenseInfo.builder()
            .accountType(AccountType.PAID)
            .licenseUnits(DEFAULT_LICENCE_UNITS)
            .expiryTime(LicenseUtils.getDefaultPaidExpiryTime())
            .accountStatus(AccountStatus.ACTIVE)
            .build());

    marketPlace.setOrderQuantity(DEFAULT_LICENCE_UNITS);
    marketPlace.setExpirationDate(new Date(LicenseUtils.getDefaultPaidExpiryTime()));
    wingsPersistence.save(marketPlace);

    // Create billing scheduler entry
    final Instant nextIteration = Instant.now().plus(1, ChronoUnit.HOURS);
    gcpBillingPollingService.create(new GCPBillingJobEntity(
        marketPlace.getAccountId(), marketPlace.getCustomerIdentificationCode(), nextIteration.toEpochMilli()));
  }

  public void handleEntitlementPlanChangeRequested(ProcurementPubsubMessage pubsubMessage) {
    gcpProcurementService.approveEntitlementPlanChange(pubsubMessage.getEntitlement());
  }

  public void handleEntitlementPlanChange(MarketPlace marketPlace, ProcurementPubsubMessage pubsubMessage) {
    // Updates an existing customer Account in the database with information from new Entitlement Plan
    String newPlan = pubsubMessage.getEntitlement().getNewPlan();
    int newOrderQuantity = DEFAULT_LICENCE_UNITS;
    Date newExpirationDate = new Date(LicenseUtils.getDefaultPaidExpiryTime());
    logger.info(
        "Updating Entitlement Plan, GCP AccountId: {}, Harness AccountId: {}. New plan: {}. Updating orderQuantity from [{}] to [{}], updating expirationDate from [{}] to [{}]",
        marketPlace.getCustomerIdentificationCode(), marketPlace.getAccountId(), newPlan,
        marketPlace.getOrderQuantity(), newOrderQuantity, marketPlace.getExpirationDate(), newExpirationDate);

    // Modify Account Licence according to information from new Entitlement Plan
    licenseService.updateAccountLicense(marketPlace.getAccountId(),
        LicenseInfo.builder()
            .accountType(AccountType.PAID)
            .licenseUnits(newOrderQuantity)
            .accountStatus(AccountStatus.ACTIVE)
            .expiryTime(newExpirationDate.getTime())
            .build());

    marketPlace.setOrderQuantity(newOrderQuantity);
    marketPlace.setExpirationDate(newExpirationDate);
    wingsPersistence.save(marketPlace);
  }

  public void handleEntitlementCancelled(MarketPlace marketPlace, ProcurementPubsubMessage pubsubMessage) {
    logger.info("GCP Account: {}, Harness Account: {}, Canceling Entitlement {}",
        marketPlace.getCustomerIdentificationCode(), marketPlace.getAccountId(),
        pubsubMessage.getEntitlement().getId());
    deactivateAccountUponEntitlementDeactivation(marketPlace);
  }

  public void handleEntitlementDeleted(MarketPlace marketPlace, ProcurementPubsubMessage pubsubMessage) {
    logger.info("GCP Account: {}, Harness Account: {}, Deleting Entitlement {}",
        marketPlace.getCustomerIdentificationCode(), marketPlace.getAccountId(),
        pubsubMessage.getEntitlement().getId());
    deactivateAccountUponEntitlementDeactivation(marketPlace);
  }

  private void deactivateAccountUponEntitlementDeactivation(MarketPlace marketPlace) {
    Date now = new Date();
    licenseService.updateAccountLicense(marketPlace.getAccountId(),
        LicenseInfo.builder()
            .accountType(AccountType.PAID)
            .licenseUnits(0)
            .accountStatus(AccountStatus.INACTIVE)
            .expiryTime(now.getTime())
            .build());

    marketPlace.setOrderQuantity(0);
    marketPlace.setExpirationDate(now);
    wingsPersistence.save(marketPlace);
  }
}
