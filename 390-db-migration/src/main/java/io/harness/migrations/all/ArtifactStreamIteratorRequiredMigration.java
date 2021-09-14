/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
