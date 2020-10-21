package io.harness.marketplace.gcp.procurement;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloudcommerceprocurement.v1.model.Account;
import com.google.cloudcommerceprocurement.v1.model.Entitlement;
import com.google.gson.Gson;
import com.google.inject.Singleton;
import com.google.pubsub.v1.PubsubMessage;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GcpMarketplaceException;
import io.harness.marketplace.gcp.procurement.pubsub.ProcurementPubsubMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.AccountStatus;
import software.wings.beans.marketplace.gcp.GCPMarketplaceCustomer;
import software.wings.beans.marketplace.gcp.GCPMarketplaceCustomer.GCPMarketplaceCustomerKeys;
import software.wings.beans.marketplace.gcp.GCPMarketplaceProduct;
import software.wings.beans.marketplace.gcp.GCPMarketplaceProduct.GCPMarketplaceProductBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@OwnedBy(PL)
@Slf4j
@Singleton
public class GcpMarketplaceMessageReceiver implements MessageReceiver {
  private final WingsPersistence wingsPersistence;
  private final AccountService accountService;
  private final GcpProcurementService gcpProcurementService;
  private final GcpProductsRegistry gcpProductsRegistry;
  private final Gson gson = new Gson();

  public GcpMarketplaceMessageReceiver(GcpProcurementService gcpProcurementService, WingsPersistence wingsPersistence,
      AccountService accountService, GcpProductsRegistry gcpProductsRegistry) {
    this.gcpProcurementService = gcpProcurementService;
    this.wingsPersistence = wingsPersistence;
    this.accountService = accountService;
    this.gcpProductsRegistry = gcpProductsRegistry;
  }

  @Override
  public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
    logger.info("Received GCP marketplace message: {}", message.getData().toStringUtf8());

    boolean ack;
    try {
      ack = processPubsubMessage(parseMessage(message));
    } catch (IOException e) {
      throw new GcpMarketplaceException(
          String.format("Failed to handle GCP marketplace message %s.", message.getMessageId()), e);
    }

    if (ack) {
      consumer.ack();
    }
  }

  private ProcurementPubsubMessage parseMessage(PubsubMessage message) {
    String data = message.getData().toStringUtf8();

    if (StringUtils.isNotBlank(data)) {
      return gson.fromJson(data, ProcurementPubsubMessage.class);
    }
    return null;
  }

  public boolean processPubsubMessage(ProcurementPubsubMessage message) throws IOException {
    if (message == null) {
      return true;
    }

    if (message.getAccount() != null && StringUtils.isNotBlank(message.getAccount().getId())) {
      return processAccount(message.getAccount());
    }

    if (message.getEntitlement() != null && StringUtils.isNotBlank(message.getEntitlement().getId())) {
      return processEntitlement(message.getEntitlement(), message.getEventType());
    }

    return false;
  }

  public boolean processAccount(ProcurementPubsubMessage.AccountMessage accountMessage) throws IOException {
    String gcpAccountId = accountMessage.getId();
    GCPMarketplaceCustomer gcpMarketplaceCustomer = getCustomer(gcpAccountId);
    Account account = gcpProcurementService.getAccount(gcpAccountId);
    if (account == null) {
      // GCP account doesn't exist anymore so we should delete Harness account mapped to this GCP account
      if (gcpMarketplaceCustomer != null) {
        logger.info("Deleting Account provisioned through GCP Marketplace with accountId: {} and GCP AccountId: {}.",
            gcpMarketplaceCustomer.getHarnessAccountId(), gcpAccountId);
        accountService.deleteAccount(gcpMarketplaceCustomer.getHarnessAccountId());
        wingsPersistence.delete(GCPMarketplaceCustomer.class, gcpMarketplaceCustomer.getUuid());
        return true;
      } else {
        return false;
      }
    }
    return true;
  }

  public boolean processEntitlement(ProcurementPubsubMessage.EntitlementMessage entitlementMessage,
      ProcurementEventType eventType) throws IOException {
    String entitlementId = entitlementMessage.getId();

    Entitlement entitlement = gcpProcurementService.getEntitlement(entitlementId);

    if (entitlement == null) {
      logger.warn(
          "Received '{}' event from GCP marketplace for entitlement that doesn't exists anymore, entitlementId: {}",
          eventType, entitlementId);
      return true;
    }

    String gcpAccountId = GcpProcurementService.getAccountId(entitlement.getAccount());
    GCPMarketplaceCustomer customer = getCustomer(gcpAccountId);

    if (customer == null) {
      return false;
    }

    switch (eventType) {
      case ENTITLEMENT_CREATION_REQUESTED:
        if (entitlement.getState().equals("ENTITLEMENT_ACTIVATION_REQUESTED")) {
          gcpProcurementService.approveEntitlement(entitlementId);
          return true;
        }
        break;
      case ENTITLEMENT_ACTIVE:
      case ENTITLEMENT_PLAN_CHANGED:
        if (entitlement.getState().equals("ENTITLEMENT_ACTIVE")) {
          updateCustomer(customer, entitlement);
          return true;
        }
        break;
      case ENTITLEMENT_PLAN_CHANGE_REQUESTED:
        if (entitlement.getState().equals("ENTITLEMENT_PENDING_PLAN_CHANGE_APPROVAL")) {
          gcpProcurementService.approveEntitlementPlanChange(entitlementId, entitlement.getNewPendingPlan());
          return true;
        }
        break;
      case ENTITLEMENT_CANCELLED:
        if (entitlement.getState().equals("ENTITLEMENT_CANCELLED")) {
          cancelCustomer(customer, entitlement);
          return true;
        }
        break;
      case ENTITLEMENT_PLAN_CHANGE_CANCELLED:
      case ENTITLEMENT_PENDING_CANCELLATION:
      case ENTITLEMENT_CANCELLATION_REVERTED:
      case ENTITLEMENT_CANCELLING:
      case ENTITLEMENT_DELETED:
        return true;
      default:
        throw new IllegalStateException("Unexpected value for GCP marketplace eventType: " + eventType);
    }

    return false;
  }

  private GCPMarketplaceCustomer getCustomer(String gcpAccountId) {
    return wingsPersistence.createQuery(GCPMarketplaceCustomer.class)
        .filter(GCPMarketplaceCustomerKeys.gcpAccountId, gcpAccountId)
        .get();
  }

  private void updateCustomer(GCPMarketplaceCustomer customer, Entitlement entitlement) throws IOException {
    GCPMarketplaceProductBuilder product = GCPMarketplaceProduct.builder();
    String productName = gcpProcurementService.getProductNameFromEntitlement(entitlement.getName());
    product.product(productName);
    product.plan(entitlement.getPlan());
    product.startTime(Instant.parse(entitlement.getCreateTime()));
    String usageReportingId = entitlement.getUsageReportingId();
    if (StringUtils.isNotBlank(usageReportingId)) {
      product.usageReportingId(usageReportingId);
    }

    Optional<GCPMarketplaceProduct> gcpMarketplaceProduct = getProduct(customer, productName);
    if (gcpMarketplaceProduct.isPresent()) {
      getGcpProductHandler(productName).handlePlanChange(customer.getHarnessAccountId(), entitlement.getPlan());
      customer.getProducts().remove(gcpMarketplaceProduct.get());
    } else {
      getGcpProductHandler(productName).handleNewSubscription(customer.getHarnessAccountId(), entitlement.getPlan());
    }
    customer.getProducts().add(product.build());
    wingsPersistence.save(customer);
  }

  private GcpProductHandler getGcpProductHandler(String productName) {
    return gcpProductsRegistry.getGcpProductHandler(productName);
  }

  private Optional<GCPMarketplaceProduct> getProduct(GCPMarketplaceCustomer customer, String productName) {
    for (GCPMarketplaceProduct subscribedProduct : customer.getProducts()) {
      if (subscribedProduct.getProduct().equals(productName)) {
        return Optional.of(subscribedProduct);
      }
    }

    return Optional.empty();
  }

  private void cancelCustomer(GCPMarketplaceCustomer customer, Entitlement entitlement) throws IOException {
    String productName = gcpProcurementService.getProductNameFromEntitlement(entitlement.getName());
    getGcpProductHandler(productName).handleSubscriptionCancellation(customer.getHarnessAccountId());

    Optional<GCPMarketplaceProduct> productToCancel = getProduct(customer, productName);
    productToCancel.ifPresent(gcpMarketplaceProduct -> customer.getProducts().remove(gcpMarketplaceProduct));

    if (customer.getProducts().isEmpty()) {
      // deactivate Harness account if there is no active products left
      accountService.updateAccountStatus(customer.getHarnessAccountId(), AccountStatus.INACTIVE);
    }
    wingsPersistence.save(customer);
  }
}
