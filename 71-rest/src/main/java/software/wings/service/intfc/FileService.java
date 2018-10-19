package software.wings.service.intfc;

import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import software.wings.beans.BaseFile;
import software.wings.beans.FileMetadata;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * The Interface FileService.
 */
public interface FileService {
  String saveFile(FileMetadata fileMetadata, InputStream in, FileBucket fileBucket);

  boolean updateParentEntityIdAndVersion(Class entityClass, String entityId, Integer version, String fileId,
      Map<String, Object> others, FileBucket fileBucket);

  String saveFile(BaseFile baseFile, InputStream uploadedInputStream, FileBucket fileBucket);

  void deleteFile(String fileId, FileBucket fileBucket);

  File download(String fileId, File file, FileBucket fileBucket);

  void downloadToStream(String fileId, OutputStream op, FileBucket fileBucket);

  InputStream openDownloadStream(String fileId, FileBucket fileBucket);

  GridFSFile getGridFsFile(String fileId, FileBucket fileBucket);

  List<String> getAllFileIds(String entityId, FileBucket fileBucket);

  String getLatestFileId(String entityId, FileBucket fileBucket);

  String getFileIdByVersion(String entityId, int version, FileBucket fileBucket);

  String uploadFromStream(String filename, BoundedInputStream in, FileBucket fileBucket, Map<String, Object> metaData);

  List<DBObject> getFilesMetaData(List<String> fileIDs, FileBucket fileBucket);

  void deleteAllFilesForEntity(String entityId, FileBucket fileBucket);

  enum FileBucket {
    LOB,
    ARTIFACTS,
    AUDITS,
    CONFIGS,
    LOGS,
    PLATFORMS,
    TERRAFORM_STATE,
    PROFILE_RESULTS;

    private int chunkSize;

    /**
     * Instantiates a new file bucket.
     *
     * @param chunkSize  the chunk size
     */
    FileBucket(int chunkSize) {
      this.chunkSize = chunkSize;
    }

    /**
     * Instantiates a new file bucket.
     */
    FileBucket() {
      this(1000 * 1000);
    }

    public String representationName() {
      return name().toLowerCase();
    }

    /**
     * Gets chunk size.
     *
     * @return the chunk size
     */
    public int getChunkSize() {
      return chunkSize;
    }
  }
}
