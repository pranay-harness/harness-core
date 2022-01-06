/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;

public class PerpetualTaskIteratorMigration implements Migration {
  @Inject private HPersistence persistence;

  @Override
  public void migrate() {
    persistence.update(
        persistence.createQuery(PerpetualTaskRecord.class).filter(PerpetualTaskRecordKeys.rebalanceIteration, null),
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.rebalanceIteration, 0));
  }
}
