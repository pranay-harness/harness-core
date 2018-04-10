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
  /**
   * Save file.
   *
   * @param fileMetadata the file metadata
   * @param in           the in
   * @param fileBucket   the file bucket
   * @return the string
   */
  String saveFile(FileMetadata fileMetadata, InputStream in, FileBucket fileBucket);

  /**
   * Update parent entity id boolean.
   *
   * @param entityId   the entity id
   * @param entityClass   the entity class
   * @param fileId     the file id
   * @param fileBucket the file bucket
   * @return the boolean
   */
  boolean updateParentEntityIdAndVersion(
      Class entityClass, String entityId, Integer version, String fileId, FileBucket fileBucket);
  /**
   * Save file.
   *
   * @param baseFile            the base file
   * @param uploadedInputStream the uploaded input stream
   * @param fileBucket          the file bucket
   * @return the string
   */
  String saveFile(BaseFile baseFile, InputStream uploadedInputStream, FileBucket fileBucket);

  /**
   * Delete file.
   *
   * @param fileId     the file id
   * @param fileBucket the file bucket
   */
  void deleteFile(String fileId, FileBucket fileBucket);

  /**
   * Download.
   *
   * @param fileId     the file id
   * @param file       the file
   * @param fileBucket the file bucket
   * @return the file
   */
  File download(String fileId, File file, FileBucket fileBucket);

  /**
   * Download to stream.
   *
   * @param fileId     the file id
   * @param op         the op
   * @param fileBucket the file bucket
   */
  void downloadToStream(String fileId, OutputStream op, FileBucket fileBucket);

  /**
   * Open download stream input stream.
   *
   * @param fileId     the file id
   * @param fileBucket the file bucket
   * @return the input stream
   */
  InputStream openDownloadStream(String fileId, FileBucket fileBucket);

  /**
   * Gets the grid fs file.
   *
   * @param fileId     the file id
   * @param fileBucket the file bucket
   * @return the grid fs file
   */
  GridFSFile getGridFsFile(String fileId, FileBucket fileBucket);

  /**
   * Gets all file ids.
   *
   * @param entityId   the entity id
   * @param fileBucket the file bucket
   * @return the all file ids
   */
  List<String> getAllFileIds(String entityId, FileBucket fileBucket);

  /**
   * Gets the latest file id.
   *
   * @param entityId   the entity id
   * @param fileBucket the file bucket
   * @return the all file ids
   */
  String getLatestFileId(String entityId, FileBucket fileBucket);

  String getFileIdByVersion(String entityId, int version, FileBucket fileBucket);

  /**
   * Upload from stream string.
   *
   * @param filename   the filename
   * @param in         the in
   * @param fileBucket the file bucket
   * @param metaData   the meta data
   * @return the string
   */
  String uploadFromStream(String filename, BoundedInputStream in, FileBucket fileBucket, Map<String, Object> metaData);

  /**
   * Gets the files meta data.
   *
   * @param fileIDs    the file i ds
   * @param fileBucket the file bucket
   * @return the files meta data
   */
  List<DBObject> getFilesMetaData(List<String> fileIDs, FileBucket fileBucket);

  /**
   * Delete all files for entity.
   *
   * @param entityId   the entity id
   * @param fileBucket the file bucket
   */
  void deleteAllFilesForEntity(String entityId, FileBucket fileBucket);

  /**
   * The Enum FileBucket.
   */
  enum FileBucket {
    /**
     * Lob file bucket.
     */
    LOB(),
    /**
     * Artifacts file bucket.
     */
    ARTIFACTS(),
    /**
     * Audits file bucket.
     */
    AUDITS(),
    /**
     * Configs file bucket.
     */
    CONFIGS(),
    /**
     * Logs file bucket.
     */
    LOGS(),
    /**
     * Platforms file bucket.
     */
    PLATFORMS(),
    /**
     * Terraform state file bucket.
     */
    TERRAFORM_STATE();

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
