/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
