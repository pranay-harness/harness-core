package io.harness.serializer.morphia;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitopsprovider.entity.ConnectedArgoProvider;
import io.harness.gitopsprovider.entity.GitOpsProvider;
import io.harness.gitopsprovider.entity.ManagedGitOpsProvider;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

@OwnedBy(HarnessTeam.GITOPS)
public class GitOpsMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(GitOpsProvider.class);
    set.add(ConnectedArgoProvider.class);
    set.add(ManagedGitOpsProvider.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
