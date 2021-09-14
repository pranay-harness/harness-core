/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logstreaming;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.network.SafeHttpCall;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.steps.StepUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class LogStreamingStepClientFactory {
  @Inject LogStreamingServiceConfiguration logStreamingServiceConfiguration;
  @Inject LogStreamingClient logStreamingClient;
  @Inject LogStreamingServiceRestClient logStreamingServiceRestClient;

  public LoadingCache<String, String> accountIdToTokenCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build(new CacheLoader<String, String>() {
            @Override
            public String load(@NotNull String accountId) throws IOException {
              return retrieveLogStreamingAccountToken(accountId);
            }
          });

  public ILogStreamingStepClient getLogStreamingStepClient(Ambiance ambiance) {
    return getLogStreamingStepClientInternal(ambiance, new HashSet<>());
  }

  private ILogStreamingStepClient getLogStreamingStepClientInternal(Ambiance ambiance, Set<String> secrets) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    try {
      return LogStreamingStepClientImpl.builder()
          .logStreamingClient(logStreamingClient)
          .logStreamingSanitizer(LogStreamingSanitizer.builder().secrets(secrets).build())
          .baseLogKey(LogStreamingHelper.generateLogBaseKey(StepUtils.generateLogAbstractions(ambiance)))
          .accountId(accountId)
          .token(accountIdToTokenCache.get(accountId))
          .build();
    } catch (Exception exception) {
      throw new InvalidRequestException("Could not generate token for given account Id" + accountId);
    }
  }

  private String retrieveLogStreamingAccountToken(String accountId) throws IOException {
    return SafeHttpCall.executeWithExceptions(logStreamingServiceRestClient.retrieveAccountToken(
        logStreamingServiceConfiguration.getServiceToken(), accountId));
  }
}
