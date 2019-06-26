package io.harness.marketplace.gcp.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.marketplace.gcp.events.intfc.Event;
import lombok.Value;

/**
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
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountActiveEvent implements Event {
  @Value
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Account {
    private String id; // account Id sent by GCP
    private String updateTime;
  }

  private String eventId;
  private EventType eventType;
  private Account account;
}
