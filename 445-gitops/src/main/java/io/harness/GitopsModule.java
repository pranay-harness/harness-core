/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;
import io.harness.gitopsprovider.mappers.ConnectedGitOpsProviderEntityMapper;
import io.harness.gitopsprovider.mappers.GitOpsProviderEntityMapper;
import io.harness.gitopsprovider.mappers.ManagedGitOpsProviderEntityMapper;
import io.harness.gitopsprovider.services.GitopsProviderService;
import io.harness.gitopsprovider.services.impl.GitOpsProviderServiceImpl;
import io.harness.persistence.HPersistence;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(HarnessTeam.GITOPS)
public class GitopsModule extends AbstractModule {
  private static final AtomicReference<GitopsModule> instanceRef = new AtomicReference<>();

  private GitopsModule() {}

  public static GitopsModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new GitopsModule());
    }

    return instanceRef.get();
  }
  @Override
  protected void configure() {
    requireBinding(HPersistence.class);
    bind(GitopsProviderService.class).to(GitOpsProviderServiceImpl.class);

    MapBinder<GitOpsProviderType, GitOpsProviderEntityMapper> gitopsProviderEntityMapperBinding =
        MapBinder.newMapBinder(binder(), GitOpsProviderType.class, GitOpsProviderEntityMapper.class);
    gitopsProviderEntityMapperBinding.addBinding(GitOpsProviderType.CONNECTED_ARGO_PROVIDER)
        .to(ConnectedGitOpsProviderEntityMapper.class);
    gitopsProviderEntityMapperBinding.addBinding(GitOpsProviderType.MANAGED_ARGO_PROVIDER)
        .to(ManagedGitOpsProviderEntityMapper.class);
  }
}
