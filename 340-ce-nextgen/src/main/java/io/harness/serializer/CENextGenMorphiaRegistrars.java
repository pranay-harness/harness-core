package io.harness.serializer;

import io.harness.gitsync.beans.GitProcessRequest;
import io.harness.gitsync.branching.EntityGitBranchMetadata;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class CENextGenMorphiaRegistrars implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // No class to register
  }
}
