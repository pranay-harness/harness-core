/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.ChangeSet;
import io.harness.ng.core.event.EventProtoToEntityHelper;

import com.google.inject.Singleton;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

@Singleton
@OwnedBy(DX)
public class ChangeSetSortComparator implements Comparator<ChangeSet>, Serializable {
  private final List<EntityType> sortOrder;

  public ChangeSetSortComparator(List<EntityType> sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public int compare(ChangeSet o1, ChangeSet o2) {
    final EntityType entityType1 = EventProtoToEntityHelper.getEntityTypeFromProto(o1.getEntityType());
    final EntityType entityType2 = EventProtoToEntityHelper.getEntityTypeFromProto(o2.getEntityType());
    return sortOrder.indexOf(entityType1) - sortOrder.indexOf(entityType2);
  }
}
