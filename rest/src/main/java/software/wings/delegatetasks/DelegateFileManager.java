package software.wings.delegatetasks;

import software.wings.service.intfc.FileService.FileBucket;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by rishi on 12/19/16.
 */
public interface DelegateFileManager {
  DelegateFile upload(DelegateFile delegateFile, InputStream contentSource);
  String getFileIdByVersion(FileBucket fileBucket, String entityId, int version, String accountId) throws IOException;
  InputStream downloadByFileId(FileBucket bucket, String fileId, String accountId, boolean encrypted)
      throws IOException;
  InputStream downloadByConfigFileId(String fileId, String accountId, String appId, String activityId)
      throws IOException;
  DelegateFile getMetaInfo(FileBucket fileBucket, String fileId, String accountId) throws IOException;
}
