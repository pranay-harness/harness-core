package io.harness.marketplace.gcp.procurement.pubsub;

import io.harness.marketplace.gcp.procurement.ProcurementEventType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcurementPubsubMessage {
  private ProcurementEventType eventType;
  private AccountMessage account;
  private EntitlementMessage entitlement;

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AccountMessage {
    private String id;
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class EntitlementMessage {
    private String id;
    private String newPlan;
  }
}
