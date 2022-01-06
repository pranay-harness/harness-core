/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.marketplace.gcp.procurement;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.marketplace.gcp.GcpMarketPlaceConstants;

import software.wings.beans.marketplace.gcp.GCPMarketplaceCustomer;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.cloudcommerceprocurement.v1.CloudCommercePartnerProcurementService;
import com.google.cloudcommerceprocurement.v1.model.Account;
import com.google.cloudcommerceprocurement.v1.model.ApproveAccountRequest;
import com.google.cloudcommerceprocurement.v1.model.ApproveEntitlementPlanChangeRequest;
import com.google.cloudcommerceprocurement.v1.model.ApproveEntitlementRequest;
import com.google.cloudcommerceprocurement.v1.model.Entitlement;
import com.google.cloudcommerceprocurement.v1.model.ListEntitlementsResponse;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

@OwnedBy(PL)
@Slf4j
@Singleton
public class GcpProcurementService {
  private static final String ACCOUNT_NAME_PATTERN = "providers/{}/accounts/{}";
  private static final String ENTITLEMENT_NAME_PATTERN = "providers/{}/entitlements/{}";
  private static final String PROVIDER_NAME_PATTERN = "providers/{}";

  @Inject private static ProcurementAPIClientBuilder procurementAPIClientBuilder = new ProcurementAPIClientBuilder();

  private static CloudCommercePartnerProcurementService getProcurementService() {
    Optional<CloudCommercePartnerProcurementService> partnerApiMaybe = procurementAPIClientBuilder.getInstance();
    if (partnerApiMaybe.isPresent()) {
      return partnerApiMaybe.get();
    } else {
      throw new UnexpectedException("Could not get GCP procurement API client. Can't approveRequestedEntitlement.");
    }
  }

  public void approveAccount(String gcpAccountId) {
    String accountName =
        MessageFormatter.format(ACCOUNT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, gcpAccountId).getMessage();

    ApproveAccountRequest approvalRequest = new ApproveAccountRequest();
    approvalRequest.setApprovalName(GcpMarketPlaceConstants.APPROVAL_SIGNUP_NAME);
    try {
      getProcurementService().providers().accounts().approve(accountName, approvalRequest).execute();
      log.info("Account approved. GCP Account ID: {}", gcpAccountId);
    } catch (IOException e) {
      log.error("Exception occurred while approving GCP Marketplace Account: {}", gcpAccountId, e);
    }
  }

  public void approveEntitlement(String id) throws IOException {
    String entitlementName =
        MessageFormatter.format(ENTITLEMENT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, id).getMessage();
    ApproveEntitlementRequest request = new ApproveEntitlementRequest();
    getProcurementService().providers().entitlements().approve(entitlementName, request).execute();
    log.info("Entitlement approved. Entitlement ID: {}", id);
  }

  public void approveEntitlementPlanChange(String id, String newPlan) throws IOException {
    String entitlementName =
        MessageFormatter.format(ENTITLEMENT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, id).getMessage();
    ApproveEntitlementPlanChangeRequest request = new ApproveEntitlementPlanChangeRequest();
    request.setPendingPlanName(newPlan);

    getProcurementService().providers().entitlements().approvePlanChange(entitlementName, request).execute();
    log.info("EntitlementPlanChangeRequest approved. Entitlement ID: {}", id);
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
      log.error("Exception listing entitlement for GCP account: {}", gcpAccountId, e);
    }
    return accountEntitlement;
  }

  private static Map<String, String> props(GCPMarketplaceCustomer marketPlace) {
    String gcpAccountId = marketPlace.getGcpAccountId();
    return ImmutableMap.of("gcpAccountId", gcpAccountId, "harnessAccountId", marketPlace.getHarnessAccountId());
  }

  public Account getAccount(String id) throws IOException {
    String accountName = getAccountName(id);
    try {
      return getProcurementService().providers().accounts().get(accountName).execute();
    } catch (GoogleJsonResponseException e) {
      if (e.getDetails().getCode() == 404) {
        return null;
      }
      throw e;
    }
  }

  private static String getAccountName(String id) {
    return MessageFormatter.format(ACCOUNT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, id).getMessage();
  }

  public Entitlement getEntitlement(String id) throws IOException {
    String entitlementName = getEntitlementName(id);
    try {
      return getProcurementService().providers().entitlements().get(entitlementName).execute();
    } catch (GoogleJsonResponseException e) {
      if (e.getDetails().getCode() == 404) {
        return null;
      }
      throw e;
    }
  }

  private String getEntitlementName(String id) {
    return MessageFormatter.format(ENTITLEMENT_NAME_PATTERN, GcpMarketPlaceConstants.PROJECT_ID, id).getMessage();
  }

  public static String getAccountId(String name) {
    return name.substring(name.lastIndexOf('/') + 1);
  }
}
