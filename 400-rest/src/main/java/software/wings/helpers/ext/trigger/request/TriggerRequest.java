/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
