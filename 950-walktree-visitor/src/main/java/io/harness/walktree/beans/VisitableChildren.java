/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.beans;

import io.harness.data.structure.EmptyPredicate;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitableChildren {
  @Builder.Default List<VisitableChild> visitableChildList = new ArrayList<>();

  public void add(String fieldName, Object value) {
    visitableChildList.add(VisitableChild.builder().fieldName(fieldName).value(value).build());
  }

  public boolean isEmpty() {
    return EmptyPredicate.isEmpty(visitableChildList);
  }
}
