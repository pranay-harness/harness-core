/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.schema.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.schema.beans.SupportedPossibleFieldTypes;

import java.util.Comparator;

@OwnedBy(DX)
public class SupportedPossibleFieldTypesComparator implements Comparator<SupportedPossibleFieldTypes> {
  @Override
  public int compare(SupportedPossibleFieldTypes o1, SupportedPossibleFieldTypes o2) {
    return o1.ordinal() - o2.ordinal();
  }
}
