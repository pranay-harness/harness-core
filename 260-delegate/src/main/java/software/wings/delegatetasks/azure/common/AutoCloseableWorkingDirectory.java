package software.wings.delegatetasks.azure.common;

import static io.harness.azure.model.AzureConstants.UNIX_SEPARATOR;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;

import static java.lang.String.format;

import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.filesystem.FileIo;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FilenameUtils;

public class AutoCloseableWorkingDirectory implements AutoCloseable {
  private File workingDir;

  public AutoCloseableWorkingDirectory(final String repositoryPath, final String rootWorkingDirPath) {
    initialize(repositoryPath, rootWorkingDirPath);
  }

  private void initialize(final String repositoryPath, final String rootWorkingDirPath) {
    String workingDirPath = FilenameUtils.concat(rootWorkingDirPath, UUIDGenerator.generateUuid() + UNIX_SEPARATOR);
    try {
      createDirectoryIfDoesNotExist(repositoryPath);
      createDirectoryIfDoesNotExist(rootWorkingDirPath);
      createDirectoryIfDoesNotExist(workingDirPath);
    } catch (IOException ex) {
      throw new InvalidRequestException(format("Unable to create directory, directoryPath: %s", workingDirPath), ex);
    }

    workingDir = new File(workingDirPath);
  }

  @Override
  public void close() {
    if (workingDir == null) {
      return;
    }

    try {
      FileIo.deleteDirectoryAndItsContentIfExists(workingDir.getAbsolutePath());
    } catch (IOException ex) {
      throw new InvalidRequestException(
          format("Failed to delete directory, directoryAbsolutePath: %s", workingDir.getAbsolutePath()), ex);
    }
  }

  public File workingDir() {
    return this.workingDir;
  }
}
