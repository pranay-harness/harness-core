/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitBranch;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.beans.GitSyncSettings;
import io.harness.gitsync.common.beans.GitToHarnessProgress;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlGitConfig;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

@OwnedBy(DX)
public class GitSyncMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(YamlGitConfig.class);
    set.add(GitCommit.class);
    set.add(YamlChangeSet.class);
    set.add(GitFileActivitySummary.class);
    set.add(GitSyncError.class);
    set.add(GitFileActivity.class);
    set.add(GitFileLocation.class);
    set.add(GitBranch.class);
    set.add(GitSyncSettings.class);
    set.add(GitToHarnessProgress.class);
    set.add(GitFullSyncEntityInfo.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
