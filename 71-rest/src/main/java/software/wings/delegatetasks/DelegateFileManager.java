package software.wings.delegatetasks;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import javax.validation.constraints.NotNull;

/**
 * Created by rishi on 12/19/16.
 */
public interface DelegateFileManager {
  DelegateFile upload(DelegateFile delegateFile, InputStream contentSource);
  String getFileIdByVersion(FileBucket fileBucket, String entityId, int version, String accountId) throws IOException;
  InputStream downloadByFileId(@NotNull FileBucket bucket, @NotEmpty String fileId, @NotEmpty String accountId)
      throws IOException;
  InputStream downloadByConfigFileId(String fileId, String accountId, String appId, String activityId)
      throws IOException;
  DelegateFile getMetaInfo(FileBucket fileBucket, String fileId, String accountId) throws IOException;

  // TODO: this method does not seem to belong here
  InputStream downloadArtifactByFileId(@NotNull FileBucket bucket, @NotEmpty String fileId, @NotEmpty String accountId)
      throws IOException, ExecutionException;

  InputStream downloadArtifactAtRuntime(ArtifactStreamAttributes artifactStreamAttributes, String accountId,
      String appId, String activityId, String commandUnitName, String hostName) throws IOException, ExecutionException;

  Long getArtifactFileSize(ArtifactStreamAttributes artifactStreamAttributes);

  void deleteCachedArtifacts();
}
