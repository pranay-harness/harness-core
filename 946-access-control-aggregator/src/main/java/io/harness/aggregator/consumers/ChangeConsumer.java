/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.aggregator.consumers;

import io.harness.accesscontrol.AccessControlEntity;
import io.harness.aggregator.OpType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public interface ChangeConsumer<T extends AccessControlEntity> {
  void consumeUpdateEvent(String id, T updatedEntity);

  void consumeDeleteEvent(String id);

  void consumeCreateEvent(String id, T createdEntity);

  default void consumeEvent(OpType opType, String id, T entity) {
    switch (opType) {
      case SNAPSHOT:
      case CREATE:
        consumeCreateEvent(id, entity);
        break;
      case UPDATE:
        consumeUpdateEvent(id, entity);
        break;
      case DELETE:
        consumeDeleteEvent(id);
        break;
      default:
        break;
    }
  }
}
