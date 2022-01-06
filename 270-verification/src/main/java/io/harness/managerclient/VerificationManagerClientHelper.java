/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.managerclient;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.FeatureName;
import io.harness.eraro.ErrorCode;
import io.harness.exception.VerificationOperationException;
import io.harness.network.SafeHttpCall;

import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.verification.VerificationDataAnalysisResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;
import retrofit2.Call;

@Slf4j
public class VerificationManagerClientHelper {
  @Inject private VerificationManagerClient managerClient;
  @Inject private CVActivityLogService cvActivityLogService;

  @VisibleForTesting static long INITIAL_DELAY_MS = 1000;
  private static final long MAX_DELAY_MS = 60000;
  private static final double DELAY_FACTOR = 1.57;
  @VisibleForTesting static Duration JITTER = Duration.ofSeconds(1);
  static final int MAX_ATTEMPTS = 10;

  public Map<String, Object> getManagerHeader(String accountId, String analysisVersion) {
    try {
      List<String> versions = callManagerWithRetry(managerClient.getListOfPublishedVersions(accountId)).getResource();
      Map<String, Object> headers = new HashMap<>();
      if (versions != null) {
        log.info("List of available versions is : {} and the analysisVersion is {}", versions, analysisVersion);
        if (analysisVersion != null && versions.contains(analysisVersion)) {
          log.info("Setting the version info in the manager call to {}", analysisVersion);
          headers.put("Version", analysisVersion);
        }
      }
      return headers;
    } catch (Exception ex) {
      throw new RuntimeException("Error while fetching manager header information");
    }
  }

  public void notifyManagerForVerificationAnalysis(AnalysisContext context, VerificationDataAnalysisResponse response) {
    Map<String, Object> headers = getManagerHeader(context.getAccountId(), context.getManagerVersion());
    if (response.getExecutionStatus() == ExecutionStatus.SUCCESS) {
      cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionId())
          .info("Analysis successful");
    } else {
      cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionId())
          .error("Analysis failed with error: " + response.getStateExecutionData().getErrorMsg());
    }
    callManagerWithRetry(managerClient.sendNotifyForVerificationState(headers, context.getCorrelationId(), response));
  }

  public void notifyManagerForVerificationAnalysis(
      String accountId, String correlationId, VerificationDataAnalysisResponse response) {
    callManagerWithRetry(
        managerClient.sendNotifyForVerificationState(getManagerHeader(accountId, null), correlationId, response));
  }

  public boolean isFeatureFlagEnabled(FeatureName featureName, String accountId) {
    return callManagerWithRetry(managerClient.isFeatureEnabled(featureName, accountId)).getResource();
  }

  public <T> T callManagerWithRetry(final Call<T> call) {
    final int[] retryCount = {0};

    RetryPolicy<Object> retryPolicy =
        new RetryPolicy<>()
            .handle(Exception.class)
            .withBackoff(INITIAL_DELAY_MS, MAX_DELAY_MS, ChronoUnit.MILLIS, DELAY_FACTOR)
            .withMaxAttempts(MAX_ATTEMPTS)
            .withJitter(JITTER)
            .abortOn(InternalServerError.class)
            .onFailedAttempt(event -> {
              retryCount[0]++;
              log.warn("[Retrying] Error while calling manager for call {}, retryCount: {}", call.request().toString(),
                  retryCount[0], event.getLastFailure());
            })
            .onFailure(event
                -> log.error("Error while calling manager for call {} after {} retries", call.request().toString(),
                    MAX_ATTEMPTS, event.getFailure()));
    try {
      return Failsafe.with(retryPolicy).get(() -> SafeHttpCall.execute(call.clone()));
    } catch (Exception e) {
      throw new VerificationOperationException(ErrorCode.RETRY_FAILED,
          "Exception occurred while calling manager from verification service. Exception: " + e.getMessage(), e);
    }
  }
}
