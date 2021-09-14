/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.gitsync;

import io.harness.EntityType;

import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GitSyncEntityTypeComparator implements Comparator<GitSyncEntities> {
  List<EntityType> orderingList;

  public GitSyncEntityTypeComparator(List<EntityType> orderingList) {
    this.orderingList = orderingList;
  }

  @Override
  public int compare(GitSyncEntities o1, GitSyncEntities o2) {
    Integer i1 = orderingList.indexOf(o1.getEntityType());
    Integer i2 = orderingList.indexOf(o2.getEntityType());
    if (i1 == -1 || i2 == -1) {
      // handling unknown entity type by logging.
      log.error("Unknown entity type in ordering");
    }
    return i1.compareTo(i2);
  }
}
