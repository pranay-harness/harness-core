/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ci.config;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StepImageConfig {
  String image;
  List<String> entrypoint;
}
