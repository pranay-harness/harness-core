package io.harness.beans.inputset;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
@Value
@Builder
@JsonTypeName("Webhook")
public class WebhookTriggerExecutionInputSet implements InputSet {
  @Builder.Default @NotEmpty private String payload;

  @Override
  public InputSet.Type getType() {
    return Type.Webhook;
  }
}
