package io.harness.cvng.beans.pagerduty;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequestType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("PAGERDUTY_DELETE_WEBHOOK")
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class PagerdutyDeleteWebhookRequest extends PagerDutyDataCollectionRequest {
  public static final String DSL = PagerDutyRegisterWebhookRequest.readDSL(
      "pagerduty-delete-webhook.datacollection", PagerdutyDeleteWebhookRequest.class);

  private String webhookId;

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public DataCollectionRequestType getType() {
    return DataCollectionRequestType.PAGERDUTY_DELETE_WEBHOOK;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> envVariables = new HashMap<>();
    envVariables.put("webhookId", webhookId);
    return envVariables;
  }
}
