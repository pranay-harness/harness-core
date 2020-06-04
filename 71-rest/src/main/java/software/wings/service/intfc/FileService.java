package software.wings.service.intfc;

import io.harness.delegate.service.DelegateAgentFileService;
import io.harness.stream.BoundedInputStream;
import software.wings.beans.BaseFile;
import software.wings.beans.FileMetadata;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * The Interface FileService.
 */
public interface FileService extends DelegateAgentFileService {
  String saveFile(FileMetadata fileMetadata, InputStream in, FileBucket fileBucket);

  boolean updateParentEntityIdAndVersion(Class entityClass, String entityId, Integer version, String fileId,
      Map<String, Object> others, FileBucket fileBucket);

  String saveFile(BaseFile baseFile, InputStream uploadedInputStream, FileBucket fileBucket);

  void deleteFile(String fileId, FileBucket fileBucket);

  File download(String fileId, File file, FileBucket fileBucket);

  void downloadToStream(String fileId, OutputStream op, FileBucket fileBucket);

  InputStream openDownloadStream(String fileId, FileBucket fileBucket);

  FileMetadata getFileMetadata(String fileId, FileBucket fileBucket);

  List<String> getAllFileIds(String entityId, FileBucket fileBucket);

  String getLatestFileId(String entityId, FileBucket fileBucket);

  String getFileIdByVersion(String entityId, int version, FileBucket fileBucket);

  String uploadFromStream(String filename, BoundedInputStream in, FileBucket fileBucket, Map<String, Object> metaData);

  void deleteAllFilesForEntity(String entityId, FileBucket fileBucket);
}
