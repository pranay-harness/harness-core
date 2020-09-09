package io.harness.marketplace.gcp.procurement;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloudcommerceprocurement.v1.model.Entitlement;
import com.google.inject.Singleton;
import com.google.pubsub.v1.PubsubMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.exception.GcpMarketplaceException;
import io.harness.exception.GcpMarketplaceProcurementException;
import io.harness.govern.Switch;
import io.harness.marketplace.gcp.procurement.pubsub.GcpAccountsHandler;
import io.harness.marketplace.gcp.procurement.pubsub.GcpEntitlementsHandler;
import io.harness.marketplace.gcp.procurement.pubsub.ProcurementPubsubMessage;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.MarketPlace;
import software.wings.beans.marketplace.MarketPlaceType;
import software.wings.service.intfc.marketplace.MarketPlaceService;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Singleton
public class GcpMarketplaceMessageReceiver implements MessageReceiver {
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
  private final GcpAccountsHandler gcpAccountsHandler;
  private final GcpProcurementService gcpProcurementService;
  private final GcpEntitlementsHandler gcpEntitlementsHandler;
  private final MarketPlaceService marketPlaceService;

  public GcpMarketplaceMessageReceiver(GcpAccountsHandler gcpAccountsHandler,
      GcpProcurementService gcpProcurementService, GcpEntitlementsHandler gcpEntitlementsHandler,
      MarketPlaceService marketPlaceService) {
    this.gcpAccountsHandler = gcpAccountsHandler;
    this.gcpProcurementService = gcpProcurementService;
    this.gcpEntitlementsHandler = gcpEntitlementsHandler;
    this.marketPlaceService = marketPlaceService;
  }

  @Override
  public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
    boolean ack = true;
    try {
      handleMessage(message);
    } catch (GcpMarketplaceProcurementException e) {
      logger.error("Failed to Approve GCP marketplace message: {}. Error: {}", message, e.getMessage(), e);
      ack = false;
    } catch (Exception e) {
      logger.error("Failed to process GCP marketplace message: {}. Error: {}", message, e.getMessage(), e);
    }
    if (ack) {
      consumer.ack();
    }
  }

  private void handleMessage(PubsubMessage pubsubMessage) throws IOException {
    String messageData = pubsubMessage.getData().toStringUtf8();
    String massageId = pubsubMessage.getMessageId();
    logger.info("Got GCP marketplace message. ID: {}, Data: {}", massageId, messageData);
    ProcurementPubsubMessage message = parseMessage(pubsubMessage);

    if (message.getAccount() != null) {
      processAccount(message);
      return;
    }

    if (message.getEntitlement() != null) {
      processEntitlement(message);
    }
  }

  public void processAccount(ProcurementPubsubMessage message) {
    ProcurementEventType eventType = message.getEventType();
    switch (eventType) {
      case ACCOUNT_ACTIVE:
        Switch.noop();
        break;
      case ACCOUNT_DELETED:
        handleAccountDeleted(message);
        break;
      default:
        logger.error("No handler for eventType: {}", eventType);
    }
  }

  public void processEntitlement(ProcurementPubsubMessage message) {
    ProcurementEventType eventType = message.getEventType();
    Entitlement entitlement = gcpProcurementService.getEntitlementById(message.getEntitlement().getId());
    MarketPlace marketPlace = getMarketPlace(entitlement.getAccount());
    switch (eventType) {
      case ENTITLEMENT_CREATION_REQUESTED:
        handleEntitlementCreation(message, marketPlace);
        break;
      case ENTITLEMENT_ACTIVE:
        handleEntitlementActive(marketPlace);
        break;
      case ENTITLEMENT_PLAN_CHANGE_REQUESTED:
        handleEntitlementPlanChangeRequested(message);
        break;
      case ENTITLEMENT_PLAN_CHANGED:
        handleEntitlementPlanChanged(message, marketPlace);
        break;
      case ENTITLEMENT_PLAN_CHANGE_CANCELLED:
        Switch.noop();
        break;
      case ENTITLEMENT_PENDING_CANCELLATION:
        Switch.noop();
        break;
      case ENTITLEMENT_CANCELLATION_REVERTED:
        Switch.noop();
        break;
      case ENTITLEMENT_CANCELLED:
        handleEntitlementCancelled(entitlement, marketPlace);
        break;
      case ENTITLEMENT_CANCELLING:
        Switch.noop();
        break;
      case ENTITLEMENT_DELETED:
        handleEntitlementDeleted(entitlement, marketPlace);
        break;
      default:
        logger.error("No handler for eventType: {}", eventType);
    }
  }

  private ProcurementPubsubMessage parseMessage(PubsubMessage message) throws IOException {
    return JSON_MAPPER.readValue(message.getData().toStringUtf8(), ProcurementPubsubMessage.class);
  }

  private MarketPlace getMarketPlace(String customerIdentificationCode) {
    Optional<MarketPlace> marketPlaceOptional =
        marketPlaceService.fetchMarketplace(customerIdentificationCode, MarketPlaceType.GCP);
    if (marketPlaceOptional.isPresent()) {
      return marketPlaceOptional.get();
    } else {
      throw new GcpMarketplaceException("Received Event for GCP AccountId: " + customerIdentificationCode
          + "that doesn't map to any Harness Account");
    }
  }

  private void handleAccountDeleted(ProcurementPubsubMessage message) {
    MarketPlace marketPlace = getMarketPlace(message.getAccount().getId());

    gcpAccountsHandler.handleAccountDeleteEvent(marketPlace);
  }

  private void handleEntitlementCreation(ProcurementPubsubMessage message, MarketPlace marketPlace) {
    gcpEntitlementsHandler.handleEntitlementCreation(marketPlace, message);
  }

  private void handleEntitlementActive(MarketPlace marketPlace) {
    gcpEntitlementsHandler.handleEntitlementActive(marketPlace);
  }

  private void handleEntitlementPlanChangeRequested(ProcurementPubsubMessage pubsubMessage) {
    gcpEntitlementsHandler.handleEntitlementPlanChangeRequested(pubsubMessage);
  }

  private void handleEntitlementPlanChanged(ProcurementPubsubMessage pubsubMessage, MarketPlace marketPlace) {
    logger.info(
        "GCP marketplace ENTITLEMENT_PLAN_CHANGED message received for GCP Account: {}, Harness Account: {} and entitlementId: {}.",
        marketPlace.getCustomerIdentificationCode(), marketPlace.getAccountId(),
        pubsubMessage.getEntitlement().getId());
    gcpEntitlementsHandler.handleEntitlementPlanChange(marketPlace, pubsubMessage);
  }

  private void handleEntitlementCancelled(Entitlement entitlement, MarketPlace marketPlace) {
    gcpEntitlementsHandler.handleEntitlementCancelled(marketPlace, entitlement);
  }

  private void handleEntitlementDeleted(Entitlement entitlement, MarketPlace marketPlace) {
    gcpEntitlementsHandler.handleEntitlementDeleted(marketPlace, entitlement);
  }
}
