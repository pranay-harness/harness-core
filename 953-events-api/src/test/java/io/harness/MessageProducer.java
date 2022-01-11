/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.entity_crud.account.AccountEntityChangeDTO;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.manage.GlobalContextManager;

import com.google.common.collect.ImmutableMap;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageProducer implements Runnable {
  private final RedisProducer client;
  private final String color;
  private final boolean isGitAware;

  public MessageProducer(RedisProducer client, String color, boolean isGitAware) {
    this.color = color;
    this.client = client;
    this.isGitAware = isGitAware;
  }

  @SneakyThrows
  @Override
  public void run() {
    if (!isGitAware) {
      publishMessages();
    } else {
      publishMessagesToGitAwareProducer();
    }
  }

  private void publishMessages() throws InterruptedException {
    int count = 0;
    while (true) {
      Message projectEvent;
      if (count % 3 == 0) {
        projectEvent =
            Message.newBuilder()
                .putAllMetadata(ImmutableMap.of("accountId", String.valueOf(count)))
                .setData(AccountEntityChangeDTO.newBuilder().setAccountId(String.valueOf(count)).build().toByteString())
                .build();
      } else {
        projectEvent =
            Message.newBuilder()
                .putAllMetadata(ImmutableMap.of("accountId", String.valueOf(count)))
                .setData(
                    ProjectEntityChangeDTO.newBuilder().setIdentifier(String.valueOf(count)).build().toByteString())
                .build();
      }

      String messageId = null;
      try {
        messageId = client.send(projectEvent);
        log.info("{}Pushed pid: {} in redis, received: {}{}", color, count, messageId, ColorConstants.TEXT_RESET);
      } catch (EventsFrameworkDownException e) {
        e.printStackTrace();
        log.error("{}Pushing message {} failed due to producer shutdown.{}", color, count, ColorConstants.TEXT_RESET);
        break;
      }

      count += 1;
      TimeUnit.SECONDS.sleep(1);
    }
  }

  private void publishMessagesToGitAwareProducer() throws InterruptedException {
    // Sending an event in git aware redis producer
    int count = 0;
    while (true) {
      String messageId = null;

      Message messageInGitAwareProducer =
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", String.valueOf(count)))
              .setData(ProjectEntityChangeDTO.newBuilder().setIdentifier(String.valueOf(count)).build().toByteString())
              .build();
      /*
       *  In some case the git context will be there in the thread and in some case it won't be.
       *  We are testing that the producer gets created in both the cases
       */
      try {
        if (count % 3 == 0) {
          final GitEntityInfo newBranch = GitEntityInfo.builder().branch("branch").yamlGitConfigId("repo").build();
          try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard()) {
            GlobalContextManager.upsertGlobalContextRecord(
                GitSyncBranchContext.builder().gitBranchInfo(newBranch).build());
            messageId = client.send(messageInGitAwareProducer);
          }
        } else {
          messageId = client.send(messageInGitAwareProducer);
        }
        log.info("{}Pushed pid: {} in redis, received: {}{}", color, count, messageId, ColorConstants.TEXT_RESET);
      } catch (EventsFrameworkDownException e) {
        e.printStackTrace();
        log.error("{}Pushing message {} failed due to producer shutdown.{}", color, count, ColorConstants.TEXT_RESET);
        break;
      }

      count += 1;
      TimeUnit.SECONDS.sleep(1);
    }
  }
}
