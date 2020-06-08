package software.wings.delegatetasks.collect.artifacts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.waiter.ListNotifyResponseData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.helpers.ext.amazons3.AmazonS3Service;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Created by rktummala on 7/30/17.
 */
@OwnedBy(CDC)
@Slf4j
public class AmazonS3CollectionTask extends AbstractDelegateRunnableTask {
  @Inject private AmazonS3Service amazonS3Service;

  public AmazonS3CollectionTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> postExecute, BooleanSupplier preExecute) {
    super(delegateTaskPackage, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    try {
      return run((AwsConfig) parameters[0], (List<EncryptedDataDetail>) parameters[1], (String) parameters[2],
          (List<String>) parameters[3]);
    } catch (Exception e) {
      logger.error("Exception occurred while collecting S3 artifacts", e);
      return new ListNotifyResponseData();
    }
  }

  public ListNotifyResponseData run(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, List<String> artifactPaths) {
    ListNotifyResponseData res = new ListNotifyResponseData();

    try {
      amazonS3Service.downloadArtifacts(
          awsConfig, encryptionDetails, bucketName, artifactPaths, getDelegateId(), getTaskId(), getAccountId());
    } catch (Exception e) {
      logger.error("Exception occurred while collecting S3 artifacts {}", ExceptionUtils.getMessage(e), e);
      // TODO: Change list
    }
    return res;
  }
}
