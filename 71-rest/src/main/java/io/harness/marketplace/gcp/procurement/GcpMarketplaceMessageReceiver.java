package io.harness.marketplace.gcp.procurement;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.inject.Singleton;
import com.google.pubsub.v1.PubsubMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.exception.GcpMarketplaceException;
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
    try {
      ProcurementPubsubMessage procurementPubsubMessage = getProcurementPubSubMessage(message);
      Optional<MarketPlace> marketPlaceOptional = getMarketPlace(procurementPubsubMessage);
      if (!marketPlaceOptional.isPresent()) {
        logger.info("Received Event for GCP Account that doesn't map to any Harness Account");
        return;
      } else {
        handleMessage(procurementPubsubMessage, marketPlaceOptional.get());
      }
    } catch (Exception e) {
      logger.error("Failed to process GCP marketplace message: {}. Error: {}", message, e.getMessage(), e);
    }
    consumer.ack();
  }

  private ProcurementPubsubMessage getProcurementPubSubMessage(PubsubMessage pubsubMessage) {
    String messageData = pubsubMessage.getData().toStringUtf8();
    String massageId = pubsubMessage.getMessageId();
    logger.info("Got GCP marketplace message. ID: {}, Data: {}", massageId, messageData);
    try {
      return parseMessage(pubsubMessage);
    } catch (IOException ioException) {
      throw new GcpMarketplaceException("Failed to parse GCP marketplace message.", ioException);
    }
  }

  private Optional<MarketPlace> getMarketPlace(ProcurementPubsubMessage message) {
    String gcpAccountId;
    if (message.getAccount() != null) {
      gcpAccountId = message.getAccount().getId();
    } else if (message.getEntitlement() != null) {
      // TODO: Handle delete entitlement (special) case
      gcpAccountId = gcpProcurementService.getEntitlementById(message.getEntitlement().getId()).getAccount();
    } else {
      throw new GcpMarketplaceException("Received GCP Marketplace event but couldn't determine GCP AccountId");
    }
    return marketPlaceService.fetchMarketplace(gcpAccountId, MarketPlaceType.GCP);
  }

  private void handleMessage(ProcurementPubsubMessage message, MarketPlace marketPlace) {
    if (message.getAccount() != null) {
      processAccount(message, marketPlace);
      return;
    }
    if (message.getEntitlement() != null) {
      processEntitlement(message, marketPlace);
    }
  }

  public void processAccount(ProcurementPubsubMessage message, MarketPlace marketPlace) {
    ProcurementEventType eventType = message.getEventType();
    switch (eventType) {
      case ACCOUNT_ACTIVE:
        Switch.noop();
        break;
      case ACCOUNT_DELETED:
        handleAccountDeleted(marketPlace);
        break;
      default:
        logger.error("No handler for eventType: {}", eventType);
    }
  }

  public void processEntitlement(ProcurementPubsubMessage message, MarketPlace marketPlace) {
    ProcurementEventType eventType = message.getEventType();
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
        handleEntitlementCancelled(message, marketPlace);
        break;
      case ENTITLEMENT_CANCELLING:
        Switch.noop();
        break;
      case ENTITLEMENT_DELETED:
        handleEntitlementDeleted(message, marketPlace);
        break;
      default:
        logger.error("No handler for eventType: {}", eventType);
    }
  }

  private ProcurementPubsubMessage parseMessage(PubsubMessage message) throws IOException {
    return JSON_MAPPER.readValue(message.getData().toStringUtf8(), ProcurementPubsubMessage.class);
  }

  private void handleAccountDeleted(MarketPlace marketPlace) {
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

  private void handleEntitlementCancelled(ProcurementPubsubMessage pubsubMessage, MarketPlace marketPlace) {
    gcpEntitlementsHandler.handleEntitlementCancelled(marketPlace, pubsubMessage);
  }

  private void handleEntitlementDeleted(ProcurementPubsubMessage pubsubMessage, MarketPlace marketPlace) {
    gcpEntitlementsHandler.handleEntitlementDeleted(marketPlace, pubsubMessage);
  }
}
