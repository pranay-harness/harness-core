package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;
import io.harness.gitopsprovider.mappers.ConnectedGitOpsProviderEntityMapper;
import io.harness.gitopsprovider.mappers.GitOpsProviderEntityMapper;
import io.harness.gitopsprovider.services.GitopsProviderService;
import io.harness.gitopsprovider.services.impl.GitOpsProviderServiceImpl;
import io.harness.persistence.HPersistence;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;

@Singleton
@OwnedBy(HarnessTeam.GITOPS)
public class GitopsModule extends AbstractModule {
  @Override
  protected void configure() {
    requireBinding(HPersistence.class);
    bind(GitopsProviderService.class).to(GitOpsProviderServiceImpl.class);

    MapBinder<GitOpsProviderType, GitOpsProviderEntityMapper> gitopsProviderEntityMapperBinding =
        MapBinder.newMapBinder(binder(), GitOpsProviderType.class, GitOpsProviderEntityMapper.class);
    gitopsProviderEntityMapperBinding.addBinding(GitOpsProviderType.CONNECTED_ARGO_PROVIDER)
        .to(ConnectedGitOpsProviderEntityMapper.class);
  }
}
