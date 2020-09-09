package io.harness.marketplace.gcp.procurement;

import com.google.cloudcommerceprocurement.v1.CloudCommercePartnerProcurementService;
import com.google.cloudcommerceprocurement.v1.model.ApproveAccountRequest;
import com.google.cloudcommerceprocurement.v1.model.ApproveEntitlementPlanChangeRequest;
import com.google.cloudcommerceprocurement.v1.model.ApproveEntitlementRequest;
import com.google.cloudcommerceprocurement.v1.model.Entitlement;
import com.google.cloudcommerceprocurement.v1.model.ListEntitlementsResponse;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.GcpMarketplaceProcurementException;
import io.harness.exception.GeneralException;
import io.harness.exception.UnexpectedException;
import io.harness.marketplace.gcp.GcpMarketPlaceConstants;
import io.harness.marketplace.gcp.procurement.pubsub.ProcurementPubsubMessage.EntitlementMessage;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedSupplier;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.web.client.HttpServerErrorException;
import software.wings.beans.MarketPlace;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class GcpProcurementService {
  private static final String ACCOUNT_NAME_PATTERN = "providers/{}/accounts/{}";
  private static final String ENTITLEMENT_NAME_PATTERN = "providers/{}/entitlements/{}";
  private static final String PROVIDER_NAME_PATTERN = "providers/{}";

  private static final long INITIAL_DELAY_MS = 500;
  private static final long MAX_DELAY_MS = 5000;
  private static final double DELAY_FACTOR = 2;
  private static final Duration JITTER_MS = Duration.ofMillis(100);
  private static final int MAX_ATTEMPTS = 3;

  @Inject private static ProcurementAPIClientBuilder procurementAPIClientBuilder = new ProcurementAPIClientBuilder();

  private static CloudCommercePartnerProcurementService getProcurementService() {
    Optional<CloudCommercePartnerProcurementService> partnerApiMaybe = procurementAPIClientBuilder.getInstance();
    if (partnerApiMaybe.isPresent()) {
      return partnerApiMaybe.get();
    } else {
      throw new UnexpectedException("Could not get GCP procurement API client. Can't approveRequestedEntitlement.");
    }
  }

  public void approveAccount(MarketPlace marketPlace) {
    String gcpAccountId = marketPlace.getCustomerIdentificationCode();

    String accountName =
        MessageFormatter.format(ACCOUNT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, gcpAccountId).getMessage();

    ApproveAccountRequest approvalRequest = new ApproveAccountRequest();
    approvalRequest.setApprovalName(GcpMarketPlaceConstants.APPROVAL_SIGNUP_NAME);
    approvalRequest.setProperties(props(marketPlace));
    try {
      getProcurementService().providers().accounts().approve(accountName, approvalRequest).execute();
      logger.info("Account approved. GCP Account ID: {}, Harness AccountId:{}. Request: {}",
          marketPlace.getCustomerIdentificationCode(), marketPlace.getAccountId(), approvalRequest);
    } catch (IOException e) {
      logger.error("Exception while approving GCP Marketplace Account: {}", marketPlace, e);
    }
  }

  public void approveEntitlement(MarketPlace marketPlace, String entitlementId) {
    String entitlementName =
        MessageFormatter.format(ENTITLEMENT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, entitlementId)
            .getMessage();
    ApproveEntitlementRequest approveEntitlementRequest = new ApproveEntitlementRequest();
    approveEntitlementRequest.setProperties(props(marketPlace));
    callProcurementServiceWithRetry(
        ()
            -> getProcurementService().providers().entitlements().approve(entitlementName, approveEntitlementRequest),
        entitlementId);
  }

  /** Approves a plan change for an Entitlement resource. */
  public void approveEntitlementPlanChange(EntitlementMessage entitlementMessage) {
    String entitlementName =
        MessageFormatter
            .format(ENTITLEMENT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, entitlementMessage.getId())
            .getMessage();
    ApproveEntitlementPlanChangeRequest request = new ApproveEntitlementPlanChangeRequest();
    request.setPendingPlanName(entitlementMessage.getNewPlan());

    callProcurementServiceWithRetry(
        ()
            -> getProcurementService().providers().entitlements().approvePlanChange(entitlementName, request).execute(),
        entitlementMessage.getId());
  }

  public List<Entitlement> listEntitlementsForGcpAccountId(String gcpAccountId) {
    List<Entitlement> accountEntitlement = new ArrayList<>();

    String providerName =
        MessageFormatter.format(PROVIDER_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID).getMessage();
    String accountName =
        MessageFormatter.format(ACCOUNT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, gcpAccountId).getMessage();

    try {
      ListEntitlementsResponse listEntitlementsResponse =
          getProcurementService().providers().entitlements().list(providerName).execute();

      List<Entitlement> entitlements = listEntitlementsResponse.getEntitlements();
      accountEntitlement = entitlements.stream()
                               .filter(entitlement -> entitlement.getAccount().equals(accountName))
                               .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error("Exception listing entitlement for GCP account: {}", gcpAccountId, e);
    }
    return accountEntitlement;
  }

  public List<Entitlement> listAllEntitlements() {
    List<Entitlement> entitlements = new ArrayList<>();
    String providerName =
        MessageFormatter.format(PROVIDER_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID).getMessage();
    try {
      ListEntitlementsResponse listEntitlementsResponse =
          getProcurementService().providers().entitlements().list(providerName).execute();
      entitlements = listEntitlementsResponse.getEntitlements();
    } catch (IOException e) {
      logger.error("Exception while listing all entitlements", e);
    }
    return entitlements;
  }

  public Entitlement getEntitlementById(String entitlementId) {
    String entitlementName =
        MessageFormatter.format(ENTITLEMENT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, entitlementId)
            .getMessage();
    try {
      Entitlement entitlement = getProcurementService().providers().entitlements().get(entitlementName).execute();
      if (null != entitlement) {
        return entitlement;
      } else {
        throw new GeneralException("Couldn't get GCP Entitlement with id: " + entitlementId);
      }
    } catch (IOException e) {
      throw new GeneralException("Exception while getting entitlement with Id: " + entitlementId, e);
    }
  }

  private static Map<String, String> props(MarketPlace marketPlace) {
    String gcpAccountId = marketPlace.getCustomerIdentificationCode();
    return ImmutableMap.of("gcpAccountId", gcpAccountId, "harnessAccountId", marketPlace.getAccountId());
  }

  private <T> T callProcurementServiceWithRetry(final CheckedSupplier<T> supplier, String entitlementId) {
    final int[] retryCount = {0};

    RetryPolicy<Object> retryPolicy =
        new RetryPolicy<>()
            .handle(Exception.class)
            .withBackoff(INITIAL_DELAY_MS, MAX_DELAY_MS, ChronoUnit.MILLIS, DELAY_FACTOR)
            .withMaxAttempts(MAX_ATTEMPTS)
            .withJitter(JITTER_MS)
            .abortOn(HttpServerErrorException.InternalServerError.class)
            .onFailedAttempt(event -> {
              retryCount[0]++;
              logger.warn("[Retrying] Error while calling GCP Procurement Service for request {}, retryCount: {}",
                  entitlementId, retryCount[0], event.getLastFailure());
            })
            .onFailure(event
                -> logger.error("Error while calling GCP Procurement Service for entitlementId {} after {} retries",
                    entitlementId, MAX_ATTEMPTS, event.getFailure()));
    try {
      return Failsafe.with(retryPolicy).get(supplier);
    } catch (Exception e) {
      throw new GcpMarketplaceProcurementException(
          "Exception occurred while calling GCP Procurement Service. Entitlement:" + entitlementId
              + "Exception: " + e.getMessage(),
          e);
    }
  }
}
