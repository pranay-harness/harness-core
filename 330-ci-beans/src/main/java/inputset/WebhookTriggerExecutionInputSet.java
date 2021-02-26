package inputset;

import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
public class WebhookTriggerExecutionInputSet implements InputSet {
  @NotEmpty private String payload;

  @Override
  public InputSet.Type getType() {
    return Type.Webhook;
  }
}
