/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.service.impl;

import io.harness.batch.processing.ccm.S3SyncRecord;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.service.intfc.AwsS3SyncService;

import software.wings.security.authentication.AwsS3SyncConfig;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.CheckedFunction0;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

/**
 * Performs aws s3 sync.
 */
@Slf4j
public class AwsS3SyncServiceImpl implements AwsS3SyncService {
  @Inject BatchMainConfig configuration;

  private static final int SYNC_TIMEOUT_MINUTES = 5;
  private static final String AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";
  private static final String AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";
  private static final String AWS_DEFAULT_REGION = "AWS_DEFAULT_REGION";
  private static final String SESSION_TOKEN = "AWS_SESSION_TOKEN";

  @Override
  @SuppressWarnings("PMD")
  public boolean syncBuckets(S3SyncRecord s3SyncRecord) {
    AwsS3SyncConfig awsCredentials = configuration.getAwsS3SyncConfig();

    // Retry class config to retry aws commands
    RetryConfig config = RetryConfig.custom()
                             .maxAttempts(5)
                             .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))
                             .retryExceptions(TimeoutException.class, InterruptedException.class, IOException.class)
                             .build();
    RetryRegistry registry = RetryRegistry.of(config);
    Retry retry = registry.retry("awsS3", config);
    Retry.EventPublisher publisher = retry.getEventPublisher();
    publisher.onRetry(event -> log.info(event.toString()));
    publisher.onSuccess(event -> log.info(event.toString()));

    ImmutableMap<String, String> envVariables = ImmutableMap.of(AWS_ACCESS_KEY_ID, awsCredentials.getAwsAccessKey(),
        AWS_SECRET_ACCESS_KEY, awsCredentials.getAwsSecretKey(), AWS_DEFAULT_REGION, awsCredentials.getRegion());
    String destinationBucketPath = null;
    try {
      final ArrayList<String> assumeRoleCmd =
          Lists.newArrayList("aws", "sts", "assume-role", "--role-arn", s3SyncRecord.getRoleArn(),
              "--role-session-name", s3SyncRecord.getAccountId(), "--external-id", s3SyncRecord.getExternalId());

      ProcessResult processResult =
          getProcessExecutor().command(assumeRoleCmd).environment(envVariables).readOutput(true).execute();
      JsonObject credentials =
          new Gson().fromJson(processResult.getOutput().getString(), JsonObject.class).getAsJsonObject("Credentials");
      ImmutableMap<String, String> roleEnvVariables =
          ImmutableMap.of(AWS_ACCESS_KEY_ID, credentials.get("AccessKeyId").getAsString(), AWS_SECRET_ACCESS_KEY,
              credentials.get("SecretAccessKey").getAsString(), AWS_DEFAULT_REGION, awsCredentials.getRegion(),
              SESSION_TOKEN, credentials.get("SessionToken").getAsString());

      JsonObject assumedRoleUser = new Gson()
                                       .fromJson(processResult.getOutput().getString(), JsonObject.class)
                                       .getAsJsonObject("AssumedRoleUser");

      String destinationBucket = s3SyncRecord.getDestinationBucket() != null ? s3SyncRecord.getDestinationBucket()
                                                                             : awsCredentials.getAwsS3BucketName();

      destinationBucketPath =
          String.join("/", "s3://" + destinationBucket, assumedRoleUser.get("AssumedRoleId").getAsString(),
              s3SyncRecord.getSettingId(), s3SyncRecord.getCurReportName());

      final ArrayList<String> cmd =
          Lists.newArrayList("aws", "s3", "sync", s3SyncRecord.getBillingBucketPath(), destinationBucketPath,
              "--source-region", s3SyncRecord.getBillingBucketRegion(), "--acl", "bucket-owner-full-control");
      log.info("cmd: {}", cmd);

      // Wrap s3 sync with a retry mechanism.
      CheckedFunction0<ProcessResult> retryingAwsS3Sync =
          Retry.decorateCheckedSupplier(retry, () -> trySyncBucket(cmd, roleEnvVariables));
      try {
        retryingAwsS3Sync.apply();
      } catch (Throwable throwable) {
        log.error("Exception during s3 sync {}", throwable);
        return false;
        // throw new BatchProcessingException("S3 sync failed", throwable);
      }
      log.info("sync completed");
    } catch (IOException | TimeoutException | InvalidExitValueException | JsonSyntaxException e) {
      log.error(e.getMessage(), e);
      log.info("Exception during s3 sync for src={}, srcRegion={}, dest={}, role-arn={}",
          s3SyncRecord.getBillingBucketPath(), s3SyncRecord.getBillingBucketRegion(), destinationBucketPath,
          s3SyncRecord.getRoleArn());
      return false;
      // throw new BatchProcessingException("S3 sync failed", e);
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
      Thread.currentThread().interrupt();
      return false;
    }
    return true;
  }

  public ProcessResult trySyncBucket(ArrayList<String> cmd, ImmutableMap<String, String> roleEnvVariables)
      throws InterruptedException, TimeoutException, IOException {
    log.info("Running the s3 sync command...");
    ProcessResult pr = getProcessExecutor()
                           .command(cmd)
                           .environment(roleEnvVariables)
                           .timeout(SYNC_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                           .redirectError(Slf4jStream.of(log).asError())
                           .exitValue(0)
                           .readOutput(true)
                           .execute();
    log.info(pr.getOutput().getUTF8());
    return pr;
  }

  ProcessExecutor getProcessExecutor() {
    return new ProcessExecutor();
  }
}
