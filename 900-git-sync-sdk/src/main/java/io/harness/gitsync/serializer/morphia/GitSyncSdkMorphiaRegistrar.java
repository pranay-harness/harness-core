package io.harness.gitsync.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.GitProcessRequest;
import io.harness.gitsync.branching.EntityGitBranchMetadata;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

@OwnedBy(DX)
public class GitSyncSdkMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(GitSyncableEntity.class);
    set.add(EntityGitBranchMetadata.class);
    set.add(GitProcessRequest.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes to register
  }
}
