package software.wings.beans.command;

import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.service.intfc.FileService.FileBucket;

/**
 * Created by anubhaw on 5/25/16.
 */
public abstract class CopyCommandUnit extends CommandUnit {
  private String fileId;
  private FileBucket fileBucket;
  private String destinationDirectoryPath;

  /**
   * Instantiates a new copy command unit.
   *
   * @param commandUnitType the command unit type
   */
  public CopyCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
  }

  /**
   * Gets file id.
   *
   * @return the file id
   */
  @SchemaIgnore
  public String getFileId() {
    return fileId;
  }

  /**
   * Sets file id.
   *
   * @param fileId the file id
   */
  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  /**
   * Gets file bucket.
   *
   * @return the file bucket
   */
  @SchemaIgnore
  public FileBucket getFileBucket() {
    return fileBucket;
  }

  /**
   * Sets file bucket.
   *
   * @param fileBucket the file bucket
   */
  public void setFileBucket(FileBucket fileBucket) {
    this.fileBucket = fileBucket;
  }

  /**
   * Gets destination file path.
   *
   * @return the destination file path
   */
  @SchemaIgnore
  public String getDestinationDirectoryPath() {
    return destinationDirectoryPath;
  }

  /**
   * Sets destination file path.
   *
   * @param destinationDirectoryPath the destination file path
   */
  public void setDestinationDirectoryPath(String destinationDirectoryPath) {
    this.destinationDirectoryPath = destinationDirectoryPath;
  }
}
