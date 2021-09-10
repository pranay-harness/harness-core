package software.wings.delegatetasks.collect.artifacts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.ListNotifyResponseData;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.beans.aws.AmazonS3CollectionTaskParameters;
import software.wings.helpers.ext.amazons3.AmazonS3Service;

import com.google.inject.Inject;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by rktummala on 7/30/17.
 */
@OwnedBy(CDC)
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AmazonS3CollectionTask extends AbstractDelegateRunnableTask {
  @Inject private AmazonS3Service amazonS3Service;

  public AmazonS3CollectionTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> postExecute, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(TaskParameters parameters) {
    try {
      AmazonS3CollectionTaskParameters amazonS3CollectionTaskParameters = (AmazonS3CollectionTaskParameters) parameters;
      return run(amazonS3CollectionTaskParameters.getAwsConfig(),
          amazonS3CollectionTaskParameters.getEncryptedDataDetails(), amazonS3CollectionTaskParameters.getJobName(),
          amazonS3CollectionTaskParameters.getArtifactPaths());
    } catch (Exception e) {
      log.error("Exception occurred while collecting S3 artifacts", e);
      return new ListNotifyResponseData();
    }
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    return run((AmazonS3CollectionTaskParameters) parameters[0]);
  }

  public ListNotifyResponseData run(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, List<String> artifactPaths) {
    ListNotifyResponseData res = new ListNotifyResponseData();

    try {
      amazonS3Service.downloadArtifacts(
          awsConfig, encryptionDetails, bucketName, artifactPaths, getDelegateId(), getTaskId(), getAccountId());
    } catch (Exception e) {
      log.error("Exception occurred while collecting S3 artifacts {}", ExceptionUtils.getMessage(e), e);
      // TODO: Change list
    }
    return res;
  }
}
