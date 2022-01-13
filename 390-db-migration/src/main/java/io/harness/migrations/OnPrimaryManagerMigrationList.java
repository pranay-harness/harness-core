/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations;

import io.harness.migrations.all.DeleteGitFileActivityAndGitFileAcitivitySummary;
import io.harness.migrations.all.RefactorTheFieldsInGitSyncError;
import io.harness.migrations.all.SyncNewFolderForConfigFiles;
import io.harness.migrations.all.TemplateLibraryYamlOnPrimaryManagerMigration;
import io.harness.migrations.gitsync.SetQueueKeyYamChangeSetMigration;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
public class OnPrimaryManagerMigrationList {
  public static List<Pair<Integer, Class<? extends OnPrimaryManagerMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends OnPrimaryManagerMigration>>>()
        .add(Pair.of(1, SyncNewFolderForConfigFiles.class))
        .add(Pair.of(2, TemplateLibraryYamlOnPrimaryManagerMigration.class))
        .add(Pair.of(3, RefactorTheFieldsInGitSyncError.class))
        .add(Pair.of(4, BaseMigration.class))
        .add(Pair.of(5, SetQueueKeyYamChangeSetMigration.class))
        .add(Pair.of(6, DeleteGitFileActivityAndGitFileAcitivitySummary.class))
        .build();
  }
}
