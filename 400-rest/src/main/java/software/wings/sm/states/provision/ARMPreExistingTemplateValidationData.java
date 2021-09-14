/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states.provision;

import software.wings.api.arm.ARMPreExistingTemplate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ARMPreExistingTemplateValidationData {
  private boolean isValidData;
  private String errorMessage;
  private ARMPreExistingTemplate preExistingTemplate;
}
