package io.harness.serializer.morphia;

import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlGitConfig;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class GitSyncMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(YamlGitConfig.class);
    set.add(YamlChangeSet.class);
    set.add(GitCommit.class);
    set.add(GitFileActivitySummary.class);
    set.add(GitFileLocation.class);
    set.add(GitSyncError.class);
    set.add(GitFileActivity.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
