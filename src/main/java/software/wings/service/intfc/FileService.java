package software.wings.service.intfc;

import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import software.wings.beans.BaseFile;
import software.wings.beans.FileMetadata;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

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
   * Gets the grid fs file.
   *
   * @param fileId     the file id
   * @param fileBucket the file bucket
   * @return the grid fs file
   */
  GridFSFile getGridFsFile(String fileId, FileBucket fileBucket);

  /**
   * Upload from stream.
   *
   * @param filename   the filename
   * @param in         the in
   * @param fileBucket the file bucket
   * @param options    the options
   * @return the string
   */
  String uploadFromStream(String filename, InputStream in, FileBucket fileBucket, GridFSUploadOptions options);

  /**
   * Gets the files meta data.
   *
   * @param fileIDs    the file i ds
   * @param fileBucket the file bucket
   * @return the files meta data
   */
  List<DBObject> getFilesMetaData(List<String> fileIDs, FileBucket fileBucket);

  /**
   * The Enum FileBucket.
   */
  enum FileBucket {
    /**
     * Lob file bucket.
     */
    LOB("lob"), /**
                 * Artifacts file bucket.
                 */
    ARTIFACTS("artifacts"), /**
                             * Audits file bucket.
                             */
    AUDITS("audits"), /**
                       * Configs file bucket.
                       */
    CONFIGS("configs"), /**
                         * Logs file bucket.
                         */
    LOGS("logs"), /**
                   * Platforms file bucket.
                   */
    PLATFORMS("platforms");

    private String bucketName;
    private int chunkSize;

    /**
     * Instantiates a new file bucket.
     *
     * @param bucketName the bucket name
     * @param chunkSize  the chunk size
     */
    FileBucket(String bucketName, int chunkSize) {
      this.bucketName = bucketName;
      this.chunkSize = chunkSize;
    }

    /**
     * Instantiates a new file bucket.
     *
     * @param bucketName the bucket name
     */
    FileBucket(String bucketName) {
      this(bucketName, 1 * 1000 * 1000);
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
      return bucketName;
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
