package software.wings.delegatetasks;

import software.wings.service.intfc.FileService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by rishi on 12/19/16.
 */
public interface DelegateFileManager {
  public DelegateFile upload(DelegateFile delegateFile, File content) throws IOException;
  public DelegateFile upload(DelegateFile delegateFile, InputStream contentSource) throws IOException;
  public DelegateFile downloadByFileId(FileService.FileBucket bucket, String fileId);
  public DelegateFile downloadByEntityId(FileService.FileBucket bucket, String entityId);
}
