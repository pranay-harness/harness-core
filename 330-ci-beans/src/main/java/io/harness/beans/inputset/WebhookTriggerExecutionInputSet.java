/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
