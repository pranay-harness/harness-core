package io.harness.gitsync.events;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_CONFIG_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_CONFIG_STREAM_PROCESSING_TIME;
import static io.harness.gitsync.AbstractGitSyncSdkModule.GIT_SYNC_SDK;

import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(DX)
public class GitSyncEventConsumerService implements Managed {
  @Inject private GitSyncConfigStreamConsumer gitSyncConfigStreamConsumer;
  private ExecutorService gitSyncConfigConsumerService;

  @Override
  public void start() throws Exception {
    gitSyncConfigConsumerService = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat(GIT_CONFIG_STREAM + GIT_SYNC_SDK).build());
    gitSyncConfigConsumerService.execute(gitSyncConfigStreamConsumer);
  }

  @Override
  public void stop() throws Exception {
    gitSyncConfigConsumerService.shutdownNow();
    gitSyncConfigConsumerService.awaitTermination(GIT_CONFIG_STREAM_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
  }
}