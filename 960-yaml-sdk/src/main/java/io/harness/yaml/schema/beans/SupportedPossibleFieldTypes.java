/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.schema.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
@OwnedBy(DX)
public enum SupportedPossibleFieldTypes {
  string,
  number,
  integer,
  bool,
  list,
  map,
  runtime, // to support runtime field type, like <+input>
  /**
   * Only used for setting default.
   */
  none
}
