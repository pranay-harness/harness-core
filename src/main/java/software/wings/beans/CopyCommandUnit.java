package software.wings.beans;

import software.wings.service.intfc.FileService.FileBucket;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/25/16.
 */
public abstract class CopyCommandUnit extends CommandUnit {
  private String fileId;
  private FileBucket fileBucket;
  private String destinationFilePath;

  /**
   * Instantiates a new copy command unit.
   *
   * @param commandUnitType the command unit type
   */
  public CopyCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
  }

  public String getFileId() {
    return fileId;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  public FileBucket getFileBucket() {
    return fileBucket;
  }

  public void setFileBucket(FileBucket fileBucket) {
    this.fileBucket = fileBucket;
  }

  public String getDestinationFilePath() {
    return destinationFilePath;
  }

  public void setDestinationFilePath(String destinationFilePath) {
    this.destinationFilePath = destinationFilePath;
  }
}
