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
  runtime,
  /**
   * Only used for setting default.
   */
  none
}
