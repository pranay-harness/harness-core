package io.harness.batch.processing.service.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;

import io.harness.batch.processing.BatchProcessingException;
import io.harness.batch.processing.ccm.S3SyncRecord;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.service.intfc.AwsS3SyncService;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import software.wings.security.authentication.AwsS3SyncConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
  public void syncBuckets(S3SyncRecord s3SyncRecord) {
    AwsS3SyncConfig awsCredentials = configuration.getAwsS3SyncConfig();
    ImmutableMap<String, String> envVariables = ImmutableMap.of(AWS_ACCESS_KEY_ID, awsCredentials.getAwsAccessKey(),
        AWS_SECRET_ACCESS_KEY, awsCredentials.getAwsSecretKey(), AWS_DEFAULT_REGION, awsCredentials.getRegion());

    String destinationBucketPath = String.join(
        "/", "s3://" + awsCredentials.getAwsS3BucketName(), s3SyncRecord.getAccountId(), s3SyncRecord.getSettingId());

    try {
      final ArrayList<String> assumeRoleCmd =
          Lists.newArrayList("aws", "sts", "assume-role", "--role-arn", s3SyncRecord.getRoleArn(),
              "--role-session-name", s3SyncRecord.getBillingAccountId(), "--external-id", s3SyncRecord.getExternalId());

      ProcessResult processResult =
          getProcessExecutor().command(assumeRoleCmd).environment(envVariables).readOutput(true).execute();
      JsonObject credentials =
          new Gson().fromJson(processResult.getOutput().getString(), JsonObject.class).getAsJsonObject("Credentials");
      ImmutableMap<String, String> roleEnvVariables =
          ImmutableMap.of(AWS_ACCESS_KEY_ID, credentials.get("AccessKeyId").getAsString(), AWS_SECRET_ACCESS_KEY,
              credentials.get("SecretAccessKey").getAsString(), AWS_DEFAULT_REGION, awsCredentials.getRegion(),
              SESSION_TOKEN, credentials.get("SessionToken").getAsString());

      final ArrayList<String> cmd =
          Lists.newArrayList("aws", "s3", "sync", s3SyncRecord.getBillingBucketPath(), destinationBucketPath,
              "--source-region", s3SyncRecord.getBillingBucketRegion(), "--acl", "bucket-owner-full-control");
      getProcessExecutor()
          .command(cmd)
          .environment(roleEnvVariables)
          .timeout(SYNC_TIMEOUT_MINUTES, TimeUnit.MINUTES)
          .redirectError(Slf4jStream.of(logger).asError())
          .exitValue(0)
          .execute();
    } catch (IOException | TimeoutException | InvalidExitValueException | JsonSyntaxException e) {
      logger.error("Exception during s3 sync for src={}, srcRegion={}, dest={}, role-arn{}",
          s3SyncRecord.getBillingBucketPath(), s3SyncRecord.getBillingBucketRegion(), destinationBucketPath,
          s3SyncRecord.getRoleArn());
      throw new BatchProcessingException("S3 sync failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  ProcessExecutor getProcessExecutor() {
    return new ProcessExecutor();
  }
}
