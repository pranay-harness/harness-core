package software.wings.helpers.ext.trigger.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.trigger.TriggerCommand.TriggerCommandType;

@Data
@AllArgsConstructor
public class TriggerRequest {
  @NotEmpty private TriggerCommandType triggerCommandType;
  String accountId;
  String appId;

  public TriggerRequest(TriggerCommandType triggerCommandType) {
    this.triggerCommandType = triggerCommandType;
  }
}
