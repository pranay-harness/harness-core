/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.schema.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.schema.beans.SubtypeClassMap;

import java.util.Comparator;

@OwnedBy(DX)
public class SubtypeClassMapComparator implements Comparator<SubtypeClassMap> {
  @Override
  public int compare(SubtypeClassMap o1, SubtypeClassMap o2) {
    return o1.getSubtypeEnum().compareTo(o2.getSubtypeEnum());
  }
}
