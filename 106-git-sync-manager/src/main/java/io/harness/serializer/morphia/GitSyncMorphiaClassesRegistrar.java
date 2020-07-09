package io.harness.serializer.morphia;

import io.harness.gitsync.common.beans.YamlGitConfig;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class GitSyncMorphiaClassesRegistrar implements io.harness.morphia.MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(YamlGitConfig.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
