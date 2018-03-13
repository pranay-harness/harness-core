package software.wings.delegatetasks.collect.artifacts;

import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Helper class that has common collection logic that's used by all the artifact collection tasks.
 * @author rktummala
 */
@Singleton
public class ArtifactCollectionTaskHelper {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectionTaskHelper.class);
  @Inject private DelegateFileManager delegateFileManager;

  public void addDataToResponse(Pair<String, InputStream> fileInfo, String artifactPath, ListNotifyResponseData res,
      String delegateId, String taskId, String accountId) throws FileNotFoundException {
    if (fileInfo == null) {
      throw new FileNotFoundException("Unable to get artifact for path " + artifactPath);
    }
    InputStream in = fileInfo.getValue();
    logger.info("Uploading the file {} for artifactPath", fileInfo.getKey(), artifactPath);
    DelegateFile delegateFile = aDelegateFile()
                                    .withFileName(fileInfo.getKey())
                                    .withDelegateId(delegateId)
                                    .withTaskId(taskId)
                                    .withAccountId(accountId)
                                    .build(); // TODO: more about delegate and task info
    DelegateFile fileRes = delegateFileManager.upload(delegateFile, in);
    logger.info("Uploaded the file name {} and fileUuid {} for artifactPath {}", fileInfo.getKey(), fileRes.getFileId(),
        artifactPath);
    ArtifactFile artifactFile = new ArtifactFile();
    artifactFile.setFileUuid(fileRes.getFileId());
    artifactFile.setName(fileInfo.getKey());
    res.addData(artifactFile);
  }
}
