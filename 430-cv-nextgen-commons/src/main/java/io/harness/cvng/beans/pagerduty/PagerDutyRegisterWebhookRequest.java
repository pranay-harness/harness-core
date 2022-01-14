package io.harness.cvng.beans.pagerduty;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequestType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("PAGERDUTY_REGISTER_WEBHOOK")
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "PagerDutyRegisterWebhookRequestKeys")
@OwnedBy(CV)
public class PagerDutyRegisterWebhookRequest extends PagerDutyDataCollectionRequest {
  public static final String DSL = PagerDutyRegisterWebhookRequest.readDSL(
      "pagerduty-register-webhook.datacollection", PagerDutyRegisterWebhookRequest.class);

  private String pagerDutyServiceId;
  private String url;

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public DataCollectionRequestType getType() {
    return DataCollectionRequestType.PAGERDUTY_REGISTER_WEBHOOK;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> envVariables = new HashMap<>();
    envVariables.put(PagerDutyRegisterWebhookRequestKeys.url, url);
    envVariables.put(PagerDutyRegisterWebhookRequestKeys.pagerDutyServiceId, pagerDutyServiceId);
    return envVariables;
  }
}