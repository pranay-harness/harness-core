package software.wings.delegatetasks.collect.artifacts;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by rktummala on 7/30/17.
 */
public class AmazonS3CollectionTask extends AbstractDelegateRunnableTask<ListNotifyResponseData> {
  private final Logger logger = LoggerFactory.getLogger(AmazonS3CollectionTask.class);

  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;

  @SuppressWarnings("Unused") @Inject private AmazonS3Service amazonS3Service;

  public AmazonS3CollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<ListNotifyResponseData> postExecute, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
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
    InputStream in = null;
    ListNotifyResponseData res = new ListNotifyResponseData();

    try {
      amazonS3Service.downloadArtifacts(
          awsConfig, encryptionDetails, bucketName, artifactPaths, getDelegateId(), getTaskId(), getAccountId());
    } catch (Exception e) {
      logger.error("Exception occurred while collecting S3 artifacts" + e.getMessage(), e);
      // TODO: Change list
    } finally {
      IOUtils.closeQuietly(in);
    }
    return res;
  }
}
