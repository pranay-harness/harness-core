/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.yaml;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@RecasterAlias("io.harness.pms.yaml.ParameterFieldValueWrapper")
@OwnedBy(HarnessTeam.PIPELINE)
public class ParameterFieldValueWrapper<T> {
  public static final String VALUE_FIELD = "value";

  private final T value;
}
