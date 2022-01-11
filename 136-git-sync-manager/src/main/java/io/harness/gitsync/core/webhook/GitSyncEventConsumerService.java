/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.webhook;

import static io.harness.eventsframework.EventsFrameworkConstants.GIT_BRANCH_HOOK_EVENT_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_BRANCH_HOOK_EVENT_STREAM_MAX_PROCESSING_TIME;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_PUSH_EVENT_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_PUSH_EVENT_STREAM_MAX_PROCESSING_TIME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.webhook.createbranchevent.GitBranchHookEventStreamConsumer;
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
  @Inject private GitBranchHookEventStreamConsumer gitCreateBranchEventStreamConsumer;
  @Inject private GitPushEventStreamConsumer gitPushEventStreamConsumer;
  private ExecutorService gitBranchHookEventConsumerService;
  private ExecutorService gitPushEventConsumerService;

  @Override
  public void start() throws Exception {
    gitBranchHookEventConsumerService =
        Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(GIT_BRANCH_HOOK_EVENT_STREAM).build());
    gitBranchHookEventConsumerService.execute(gitCreateBranchEventStreamConsumer);

    gitPushEventConsumerService =
        Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(GIT_PUSH_EVENT_STREAM).build());
    gitPushEventConsumerService.execute(gitPushEventStreamConsumer);
  }

  @Override
  public void stop() throws Exception {
    gitBranchHookEventConsumerService.shutdownNow();
    gitBranchHookEventConsumerService.awaitTermination(
        GIT_BRANCH_HOOK_EVENT_STREAM_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);

    gitPushEventConsumerService.shutdownNow();
    gitPushEventConsumerService.awaitTermination(
        GIT_PUSH_EVENT_STREAM_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
  }
}
