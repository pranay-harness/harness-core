package software.wings.delegatetasks.collect.artifacts;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
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
      return run((String) parameters[0], (String) parameters[1], (char[]) parameters[2], (String) parameters[3],
          (List<String>) parameters[4]);
    } catch (Exception e) {
      logger.error("Exception occurred while collecting S3 artifacts", e);
      return new ListNotifyResponseData();
    }
  }

  public ListNotifyResponseData run(
      String accountId, String accessKey, char[] secretKey, String bucketName, List<String> artifactPaths) {
    InputStream in = null;
    ListNotifyResponseData res = new ListNotifyResponseData();

    try {
      AwsConfig awsConfig = AwsConfig.builder().accountId(accountId).accessKey(accessKey).secretKey(secretKey).build();
      amazonS3Service.downloadArtifacts(
          awsConfig, bucketName, artifactPaths, getDelegateId(), getTaskId(), getAccountId());
    } catch (Exception e) {
      logger.error("Exception occurred while collecting S3 artifacts" + e.getMessage(), e);
      // TODO: Change list
    } finally {
      IOUtils.closeQuietly(in);
    }
    return res;
  }
}
