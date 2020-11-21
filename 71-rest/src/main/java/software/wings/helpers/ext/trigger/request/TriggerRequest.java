package software.wings.helpers.ext.trigger.request;

import software.wings.beans.trigger.TriggerCommand.TriggerCommandType;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

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
