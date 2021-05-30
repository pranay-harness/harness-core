package io.harness.gitsync.core.webhook;

import static io.harness.eventsframework.EventsFrameworkConstants.GIT_CREATE_BRANCH_EVENT_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_CREATE_BRANCH_EVENT_STREAM_MAX_PROCESSING_TIME;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_PUSH_EVENT_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_PUSH_EVENT_STREAM_MAX_PROCESSING_TIME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.webhook.createbranchevent.GitCreateBranchEventStreamConsumer;
import io.harness.gitsync.core.webhook.pushevent.GitPushEventStreamConsumer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.DX)
@Slf4j
public class GitSyncEventConsumerService implements Managed {
  @Inject private GitCreateBranchEventStreamConsumer gitCreateBranchEventStreamConsumer;
  @Inject private GitPushEventStreamConsumer gitPushEventStreamConsumer;
  private ExecutorService gitCreateBranchEventConsumerService;
  private ExecutorService gitPushEventConsumerService;

  @Override
  public void start() throws Exception {
    gitCreateBranchEventConsumerService = Executors.newFixedThreadPool(
        2, new ThreadFactoryBuilder().setNameFormat(GIT_CREATE_BRANCH_EVENT_STREAM).build());
    gitCreateBranchEventConsumerService.execute(gitCreateBranchEventStreamConsumer);

    gitPushEventConsumerService =
        Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(GIT_PUSH_EVENT_STREAM).build());
    gitPushEventConsumerService.execute(gitPushEventStreamConsumer);
  }

  @Override
  public void stop() throws Exception {
    gitCreateBranchEventConsumerService.shutdown();
    gitCreateBranchEventConsumerService.awaitTermination(
        GIT_CREATE_BRANCH_EVENT_STREAM_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);

    gitPushEventConsumerService.shutdown();
    gitPushEventConsumerService.awaitTermination(
        GIT_PUSH_EVENT_STREAM_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
  }
}
