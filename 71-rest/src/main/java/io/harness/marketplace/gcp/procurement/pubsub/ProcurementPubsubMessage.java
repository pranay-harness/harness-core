package io.harness.marketplace.gcp.procurement.pubsub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.marketplace.gcp.procurement.ProcurementEventType;
import lombok.Data;
import lombok.Value;

/**
 * Captures GCP marketplace pub/sub events
 *
 *
 * Sample event:
 * <pre>
 * {
 *   "eventId": "CREATE_ACCOUNT-3453c108-1ee5-474c-8905-683c1d4ce002",
 *   "eventType": "ACCOUNT_ACTIVE",
 *   "account": {
 *     "id": "E-AA4E-2AF3-EFDD-96D6",
 *     "updateTime": "2019-06-12T18:55:24.707Z"
 *   }
 * }
 * </pre>
 *
 * (Note: GCP sends both Create Account and Account Approve events with the same event type)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcurementPubsubMessage implements Event {
  private ProcurementEventType eventType;
  private String eventId;

  private AccountMessage account;
  private EntitlementMessage entitlement;

  @Value
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AccountMessage {
    String id;
    String updateTime;
  }

  @Value
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class EntitlementMessage {
    String id;
    String updateTime;
    String newPlan;
    String newProduct;
    String cancellationDate;
  }
}
