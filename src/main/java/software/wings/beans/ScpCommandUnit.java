package software.wings.beans;

import software.wings.core.ssh.executors.SshExecutor.ExecutionResult;
import software.wings.service.intfc.FileService.FileBucket;

/**
 * Created by anubhaw on 5/25/16.
 */
public class ScpCommandUnit extends CommandUnit {
  private String fileId;
  private FileBucket fileBucket;
  private String destinationFilePath;

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

  public static final class ScpCommandUnitBuilder {
    private String fileId;
    private FileBucket fileBucket;
    private String destinationFilePath;
    private CommandUnitType commandUnitType;
    private ExecutionResult executionResult;

    private ScpCommandUnitBuilder() {}

    public static ScpCommandUnitBuilder aScpCommandUnit() {
      return new ScpCommandUnitBuilder();
    }

    public ScpCommandUnitBuilder withFileId(String fileId) {
      this.fileId = fileId;
      return this;
    }

    public ScpCommandUnitBuilder withFileBucket(FileBucket fileBucket) {
      this.fileBucket = fileBucket;
      return this;
    }

    public ScpCommandUnitBuilder withDestinationFilePath(String destinationFilePath) {
      this.destinationFilePath = destinationFilePath;
      return this;
    }

    public ScpCommandUnitBuilder withCommandUnitType(CommandUnitType commandUnitType) {
      this.commandUnitType = commandUnitType;
      return this;
    }

    public ScpCommandUnitBuilder withExecutionResult(ExecutionResult executionResult) {
      this.executionResult = executionResult;
      return this;
    }

    public ScpCommandUnitBuilder but() {
      return aScpCommandUnit()
          .withFileId(fileId)
          .withFileBucket(fileBucket)
          .withDestinationFilePath(destinationFilePath)
          .withCommandUnitType(commandUnitType)
          .withExecutionResult(executionResult);
    }

    public ScpCommandUnit build() {
      ScpCommandUnit scpCommandUnit = new ScpCommandUnit();
      scpCommandUnit.setFileId(fileId);
      scpCommandUnit.setFileBucket(fileBucket);
      scpCommandUnit.setDestinationFilePath(destinationFilePath);
      scpCommandUnit.setCommandUnitType(commandUnitType);
      scpCommandUnit.setExecutionResult(executionResult);
      return scpCommandUnit;
    }
  }
}
