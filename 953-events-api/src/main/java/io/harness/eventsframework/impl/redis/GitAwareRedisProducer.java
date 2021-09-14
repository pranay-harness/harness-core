/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.eventsframework.impl.redis;

import static io.harness.NGConstants.BRANCH;
import static io.harness.NGConstants.REPO;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.manage.GlobalContextManager;
import io.harness.redis.RedisConfig;

import io.github.resilience4j.retry.Retry;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class GitAwareRedisProducer extends RedisProducer {
  private Retry retry;

  public GitAwareRedisProducer(
      String topicName, @NotNull RedisConfig redisConfig, int maxTopicSize, String producerName) {
    super(topicName, redisConfig, maxTopicSize, producerName);
  }

  @Override
  protected void populateOtherProducerSpecificData(Map<String, String> redisData) {
    // Populating the git details
    super.populateOtherProducerSpecificData(redisData);
    GlobalContextData globalContextData = GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    if (globalContextData != null) {
      final GitEntityInfo gitBranchInfo = ((GitSyncBranchContext) globalContextData).getGitBranchInfo();
      if (gitBranchInfo == null) {
        return;
      }
      redisData.put(REPO, gitBranchInfo.getYamlGitConfigId());
      redisData.put(BRANCH, gitBranchInfo.getBranch());
    }
  }

  public static GitAwareRedisProducer of(
      String topicName, @NotNull RedisConfig redisConfig, int maxTopicLength, String producerName) {
    return new GitAwareRedisProducer(topicName, redisConfig, maxTopicLength, producerName);
  }
}
