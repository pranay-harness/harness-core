/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.cdng.beans;

import lombok.Builder;
import lombok.Value;
@Value
@Builder
public class InputSetTemplateRequest {
  String pipelineYaml;
  String templateYaml; // We don't need this in the current implementation but keeping it as part of api in case future
                       // implementation changes.
}
