package io.harness.marketplace.gcp.procurement;

import com.google.cloudcommerceprocurement.v1.CloudCommercePartnerProcurementService;
import com.google.cloudcommerceprocurement.v1.model.ApproveAccountRequest;
import com.google.cloudcommerceprocurement.v1.model.ApproveEntitlementRequest;
import com.google.cloudcommerceprocurement.v1.model.Entitlement;
import com.google.cloudcommerceprocurement.v1.model.ListEntitlementsResponse;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.marketplace.gcp.GcpMarketPlaceConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import software.wings.beans.MarketPlace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class GcpProcurementService {
  private static final String ACCOUNT_NAME_PATTERN = "providers/{}/accounts/{}";
  private static final String ENTITLEMENT_NAME_PATTERN = "providers/{}/accounts/{}";
  private static final String PROVIDER_NAME_PATTERN = "providers/{}";

  @Inject private ProcurementAPIClientBuilder procurementAPIClientBuilder;

  public void approve(MarketPlace marketPlace) {
    Optional<CloudCommercePartnerProcurementService> partnerApiMaybe = procurementAPIClientBuilder.getInstance();
    CloudCommercePartnerProcurementService partnerProcurementService;
    if (!partnerApiMaybe.isPresent()) {
      logger.error("Could not get GCP procurement API client. Can't approve.");
      return;
    } else {
      partnerProcurementService = partnerApiMaybe.get();
    }

    String gcpAccountId = marketPlace.getCustomerIdentificationCode();

    String accountName =
        MessageFormatter.format(ACCOUNT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, gcpAccountId).getMessage();

    ApproveAccountRequest approvalRequest = new ApproveAccountRequest();
    approvalRequest.setApprovalName(GcpMarketPlaceConstants.APPROVAL_NAME);
    approvalRequest.setProperties(props(marketPlace));
    try {
      partnerProcurementService.providers().accounts().approve(accountName, approvalRequest).execute();
      logger.info("Account approved. Request: {}", approvalRequest);
    } catch (IOException e) {
      logger.error("Exception approving marketplace: {}", marketPlace, e);
    }
  }

  public void approveEntitlement(MarketPlace marketPlace, String entitlementId) {
    CloudCommercePartnerProcurementService partnerProcurementService;
    Optional<CloudCommercePartnerProcurementService> partnerApiMaybe = procurementAPIClientBuilder.getInstance();
    if (!partnerApiMaybe.isPresent()) {
      logger.error("Could not get GCP procurement API client. Can't approve.");
      return;
    } else {
      partnerProcurementService = partnerApiMaybe.get();
    }

    String entitlementName =
        MessageFormatter.format(ENTITLEMENT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, entitlementId)
            .getMessage();
    ApproveEntitlementRequest approveEntitlementRequest = new ApproveEntitlementRequest();
    approveEntitlementRequest.setProperties(props(marketPlace));

    try {
      partnerProcurementService.providers()
          .entitlements()
          .approve(entitlementName, approveEntitlementRequest)
          .execute();
    } catch (IOException e) {
      logger.error("Exception approving entitlement for marketplace: {}. Request: {}", marketPlace,
          approveEntitlementRequest, e);
    }
  }

  public void approveRequestedEntitlement(MarketPlace marketPlace) {
    CloudCommercePartnerProcurementService partnerProcurementService;
    Optional<CloudCommercePartnerProcurementService> partnerApiMaybe = procurementAPIClientBuilder.getInstance();
    if (!partnerApiMaybe.isPresent()) {
      logger.error("Could not get GCP procurement API client. Can't approveRequestedEntitlement.");
      return;
    } else {
      partnerProcurementService = partnerApiMaybe.get();
    }

    List<Entitlement> allEntitlements = listEntitlements(marketPlace);
    List<Entitlement> pending =
        allEntitlements.stream()
            .filter(it -> it.getState().equals(GcpMarketPlaceConstants.ENTITLEMENT_ACTIVATION_REQUESTED))
            .collect(Collectors.toList());

    if (pending.size() == 0) {
      logger.error("No pending entitlements for marketplace: {}", marketPlace);
      return;
    }

    if (pending.size() > 1) {
      logger.error(
          "More than 1 pending entitlements for marketplace. Will approve first one. Marketplace: {}", marketPlace);
    }

    Entitlement entitlementTpApprove = pending.get(0);
    try {
      ApproveEntitlementRequest approvalRequest = new ApproveEntitlementRequest();
      approvalRequest.setProperties(props(marketPlace));
      partnerProcurementService.providers()
          .entitlements()
          .approve(entitlementTpApprove.getName(), approvalRequest)
          .execute();

    } catch (IOException e) {
      logger.error("Exception listing entitlement for marketplace: {}", marketPlace, e);
    }
  }

  public List<Entitlement> listEntitlements(MarketPlace marketPlace) {
    List<Entitlement> accountEntitlement = new ArrayList<>();
    CloudCommercePartnerProcurementService partnerProcurementService;
    Optional<CloudCommercePartnerProcurementService> partnerApiMaybe = procurementAPIClientBuilder.getInstance();
    if (!partnerApiMaybe.isPresent()) {
      logger.error("Could not get GCP procurement API client. Can't list entitlement.");
      return accountEntitlement;
    } else {
      partnerProcurementService = partnerApiMaybe.get();
    }

    String gcpAccountId = marketPlace.getCustomerIdentificationCode();
    String accountName =
        MessageFormatter.format(ACCOUNT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, gcpAccountId).getMessage();

    try {
      ListEntitlementsResponse listEntitlementsResponse =
          partnerProcurementService.providers().entitlements().list(GcpMarketPlaceConstants.PROJECT_ID).execute();

      // TODO check next token
      List<Entitlement> entitlements = listEntitlementsResponse.getEntitlements();
      accountEntitlement = entitlements.stream()
                               .filter(entitlement -> entitlement.getAccount().equals(accountName))
                               .collect(Collectors.toList());

    } catch (IOException e) {
      logger.error("Exception listing entitlement: {}", marketPlace, e);
    }

    return accountEntitlement;
  }

  private static Map<String, String> props(MarketPlace marketPlace) {
    String gcpAccountId = marketPlace.getCustomerIdentificationCode();
    return ImmutableMap.of("gcpAccountId", gcpAccountId, "harnessAccountId", marketPlace.getAccountId());
  }
}
