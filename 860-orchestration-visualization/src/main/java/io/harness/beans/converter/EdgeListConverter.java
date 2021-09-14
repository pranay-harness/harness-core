/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.converter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EdgeList;
import io.harness.beans.internal.EdgeListInternal;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class EdgeListConverter {
  public EdgeList convertFrom(EdgeListInternal edgeListInternal) {
    return EdgeList.builder().edges(edgeListInternal.getEdges()).nextIds(edgeListInternal.getNextIds()).build();
  }
}
