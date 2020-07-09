package io.harness.gitsync;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;

import io.harness.gitsync.common.impl.YamlGitConfigServiceImpl;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;
import io.harness.threading.ThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class GitSyncModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(HPersistence.class).to(MongoPersistence.class);
    bind(YamlGitConfigService.class).to(YamlGitConfigServiceImpl.class);
    bind(ExecutorService.class)
        .toInstance(ThreadPool.create(1, 20, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder().setNameFormat("default-git-sync-%d").setPriority(Thread.MIN_PRIORITY).build()));

    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
