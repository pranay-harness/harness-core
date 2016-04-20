package software.wings.service.intfc;

import com.mongodb.client.gridfs.model.GridFSFile;
import software.wings.beans.FileMetadata;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public interface FileService {
  public String saveFile(FileMetadata fileMetadata, InputStream in);

  public File download(String fileId, File file);

  public void downloadToStream(String fileId, OutputStream op);

  public GridFSFile getGridFsFile(String fileId);
}
