package software.wings.service.intfc;

import java.io.File;
import java.io.InputStream;

import software.wings.beans.FileMetadata;

public interface FileService {
  public String saveFile(FileMetadata fileMetadata, InputStream in);

  public File download(String fileId, File file);
}
