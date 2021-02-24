package io.harness.logStreaming;

import io.harness.logstreaming.LogStreamingClient;
import io.harness.logstreaming.LogStreamingServiceConfiguration;
import io.harness.logstreaming.LogStreamingServiceRestClient;
import io.harness.network.SafeHttpCall;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.steps.StepUtils;

import software.wings.beans.LogHelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

@Singleton
public class LogStreamingStepClientFactory {
  @Inject LogStreamingServiceConfiguration logStreamingServiceConfiguration;
  @Inject LogStreamingClient logStreamingClient;
  @Inject LogStreamingServiceRestClient logStreamingServiceRestClient;

  public LoadingCache<Ambiance, ILogStreamingStepClient> logStreamingStepClientCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build(new CacheLoader<Ambiance, ILogStreamingStepClient>() {
            @Override
            public ILogStreamingStepClient load(@NotNull Ambiance ambiance) throws IOException {
              return getLogStreamingStepClientInternal(ambiance);
            }
          });

  public ILogStreamingStepClient getLogStreamingStepClient(Ambiance ambiance) throws ExecutionException {
    return logStreamingStepClientCache.get(ambiance);
  }

  private ILogStreamingStepClient getLogStreamingStepClientInternal(Ambiance ambiance) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    try {
      return LogStreamingStepClientImpl.builder()
          .logStreamingClient(logStreamingClient)
          .baseLogKey(LogHelper.generateLogBaseKey(StepUtils.generateLogAbstractions(ambiance)))
          .accountId(AmbianceUtils.getAccountId(ambiance))
          .token(retrieveLogStreamingAccountToken(accountId))
          .build();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    return null;
  }

  @VisibleForTesting
  protected String retrieveLogStreamingAccountToken(String accountId) throws IOException {
    return SafeHttpCall.executeWithExceptions(logStreamingServiceRestClient.retrieveAccountToken(
        logStreamingServiceConfiguration.getServiceToken(), accountId));
  }
}
