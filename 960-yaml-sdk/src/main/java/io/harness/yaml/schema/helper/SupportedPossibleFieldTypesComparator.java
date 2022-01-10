/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
