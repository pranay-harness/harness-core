/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArtifactStreamIteratorRequiredMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    wingsPersistence.update(
        wingsPersistence.createQuery(ArtifactStream.class).filter(ArtifactStreamKeys.nextIteration, null),
        wingsPersistence.createUpdateOperations(ArtifactStream.class).set(ArtifactStreamKeys.nextIteration, 0));
    wingsPersistence.update(
        wingsPersistence.createQuery(ArtifactStream.class).filter(ArtifactStreamKeys.nextCleanupIteration, null),
        wingsPersistence.createUpdateOperations(ArtifactStream.class).set(ArtifactStreamKeys.nextCleanupIteration, 0));
  }
}
