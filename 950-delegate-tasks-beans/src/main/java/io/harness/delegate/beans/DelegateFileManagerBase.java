package io.harness.delegate.beans;

import java.io.IOException;
import java.io.InputStream;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface DelegateFileManagerBase {
  DelegateFile upload(DelegateFile delegateFile, InputStream contentSource);
  String getFileIdByVersion(FileBucket fileBucket, String entityId, int version, String accountId) throws IOException;
  InputStream downloadByFileId(@NotNull FileBucket bucket, @NotEmpty String fileId, @NotEmpty String accountId)
      throws IOException;
  InputStream downloadByConfigFileId(String fileId, String accountId, String appId, String activityId)
      throws IOException;
  DelegateFile getMetaInfo(FileBucket fileBucket, String fileId, String accountId) throws IOException;
}
