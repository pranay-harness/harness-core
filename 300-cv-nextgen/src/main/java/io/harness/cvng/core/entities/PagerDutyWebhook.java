package io.harness.cvng.core.entities;

import io.harness.cvng.core.beans.WebhookType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@JsonTypeName("PAGER_DUTY")
@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "PagerDutyWebhookKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PagerDutyWebhook extends Webhook {
  @NonNull private String serviceIdentifier;
  @NonNull private String envIdentifier;
  String pagerdutyChangeSourceId;
  String webhookId;

  public WebhookType getType() {
    return WebhookType.PAGER_DUTY;
  }
}
