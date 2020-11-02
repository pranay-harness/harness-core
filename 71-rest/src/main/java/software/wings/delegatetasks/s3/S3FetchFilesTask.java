package software.wings.delegatetasks.s3;

import static java.lang.String.format;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.s3.FetchS3FilesExecutionResponse.FetchS3FilesCommandStatus.FAILURE;

import com.google.inject.Inject;

import com.amazonaws.services.s3.model.S3Object;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.CommandExecutionException;
import io.harness.exception.ExceptionUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.util.StreamUtils;
import software.wings.beans.AwsConfig;
import software.wings.beans.LogColor;
import software.wings.beans.LogWeight;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.s3.FetchS3FilesCommandParams;
import software.wings.beans.s3.FetchS3FilesExecutionResponse;
import software.wings.beans.s3.S3Bucket;
import software.wings.beans.s3.S3FetchFileResult;
import software.wings.beans.s3.S3File;
import software.wings.beans.s3.S3FileRequest;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class S3FetchFilesTask extends AbstractDelegateRunnableTask {
  @Inject private EncryptionService encryptionService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private DelegateLogService delegateLogService;

  public S3FetchFilesTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public FetchS3FilesExecutionResponse run(TaskParameters parameters) {
    FetchS3FilesCommandParams taskParams = (FetchS3FilesCommandParams) parameters;
    log.info("Running S3FetchFilesTask for account {}, app {}, activityId {}", taskParams.getAccountId(),
        taskParams.getAppId(), taskParams.getActivityId());

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(delegateLogService, taskParams.getAccountId(),
        taskParams.getAppId(), taskParams.getActivityId(), taskParams.getExecutionLogName());
    try {
      encryptionService.decrypt(taskParams.getAwsConfig(), taskParams.getEncryptionDetails(), false);
      S3FetchFileResult result = getContentFromFromS3Bucket(taskParams.getS3FileRequests(), taskParams.getAwsConfig(),
          taskParams.getEncryptionDetails(), executionLogCallback);

      executionLogCallback.saveExecutionLog(
          "Successfully fetched files", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      return FetchS3FilesExecutionResponse.builder()
          .s3FetchFileResult(result)
          .commandStatus(FetchS3FilesExecutionResponse.FetchS3FilesCommandStatus.SUCCESS)
          .build();
    } catch (Exception ex) {
      String errorMessage = format("Exception: %s while fetching s3 file.", ExceptionUtils.getMessage(ex));
      log.error(errorMessage, ex);
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);

      return FetchS3FilesExecutionResponse.builder().errorMessage(errorMessage).commandStatus(FAILURE).build();
    }
  }

  private S3FetchFileResult getContentFromFromS3Bucket(List<S3FileRequest> s3FileRequests, AwsConfig awsConfig,
      List<EncryptedDataDetail> details, ExecutionLogCallback executionLogCallback) {
    List<S3Bucket> s3Buckets = new ArrayList<>();
    s3FileRequests.forEach(s3FileRequest -> {
      List<S3File> s3Files = new ArrayList<>();
      s3FileRequest.getFileKeys().forEach(fileKey -> {
        executionLogCallback.saveExecutionLog(
            color(format("%nFetching %s file from s3 bucket: %s", fileKey, s3FileRequest.getBucketName()),
                LogColor.White, LogWeight.Bold));
        S3Object s3Object =
            awsHelperService.getObjectFromS3(awsConfig, details, s3FileRequest.getBucketName(), fileKey);
        try (InputStream is = s3Object.getObjectContent()) {
          S3File s3File = S3File.builder()
                              .fileKey(fileKey)
                              .fileContent(StreamUtils.copyToString(is, StandardCharsets.UTF_8))
                              .build();
          s3Files.add(s3File);
        } catch (IOException e) {
          String errorMsg =
              String.format("Failed to fetch %s from s3 bucket: %s", fileKey, s3FileRequest.getBucketName());
          executionLogCallback.saveExecutionLog(color(errorMsg, LogColor.White, LogWeight.Bold));
          throw new CommandExecutionException(errorMsg);
        }
      });
      s3Buckets.add(S3Bucket.builder().name(s3FileRequest.getBucketName()).s3Files(s3Files).build());
    });

    return S3FetchFileResult.builder().s3Buckets(s3Buckets).build();
  }

  @Override
  public GitCommandExecutionResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
