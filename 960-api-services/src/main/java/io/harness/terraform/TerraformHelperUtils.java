package io.harness.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.provision.TerraformConstants.TERRAFORM_INTERNAL_FOLDER;
import static io.harness.provision.TerraformConstants.TERRAFORM_STATE_FILE_NAME;
import static io.harness.provision.TerraformConstants.WORKSPACE_DIR_BASE;
import static io.harness.provision.TerraformConstants.WORKSPACE_STATE_FILE_PATH_FORMAT;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.filesystem.FileIo;

import com.google.common.base.Throwables;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.JGitInternalException;

@UtilityClass
@OwnedBy(CDP)
@Slf4j
public class TerraformHelperUtils {
  public String generateCommandFlagsString(List<String> arguments, String command) {
    StringBuilder stringargs = new StringBuilder();
    if (isNotEmpty(arguments)) {
      for (String arg : arguments) {
        stringargs.append(command).append(arg).append(' ');
      }
    }
    return stringargs.toString();
  }

  public static String getGitExceptionMessageIfExists(Throwable t) {
    if (t instanceof JGitInternalException) {
      return Throwables.getRootCause(t).getMessage();
    }
    return ExceptionUtils.getMessage(t);
  }

  public void copyFilesToWorkingDirectory(String sourceDir, String destinationDir) throws IOException {
    File dest = new File(destinationDir);
    File src = new File(sourceDir);
    FileUtils.deleteDirectory(dest);
    FileUtils.copyDirectory(src, dest);
    FileIo.waitForDirectoryToBeAccessibleOutOfProcess(dest.getPath(), 10);
  }

  public void ensureLocalCleanup(String scriptDirectory) throws IOException {
    FileUtils.deleteQuietly(Paths.get(scriptDirectory, TERRAFORM_STATE_FILE_NAME).toFile());
    try {
      deleteDirectoryAndItsContentIfExists(Paths.get(scriptDirectory, TERRAFORM_INTERNAL_FOLDER).toString());
    } catch (IOException e) {
      log.warn("Failed to delete .terraform folder");
    }
    deleteDirectoryAndItsContentIfExists(Paths.get(scriptDirectory, WORKSPACE_DIR_BASE).toString());
  }

  public File getTerraformStateFile(String scriptDirectory, String workspace) {
    File tfStateFile = isEmpty(workspace)
        ? Paths.get(scriptDirectory, TERRAFORM_STATE_FILE_NAME).toFile()
        : Paths.get(scriptDirectory, format(WORKSPACE_STATE_FILE_PATH_FORMAT, workspace)).toFile();

    return tfStateFile.exists() ? tfStateFile : null;
  }

  public List<String> createFileFromStringContent(List<String> varFilesListContent, String scriptDirectory)
      throws IOException {
    ArrayList<String> varFilesList = new ArrayList();

    for (int i = 0; i < varFilesListContent.size(); i++) {
      Path filePath = Files.createFile(Paths.get(scriptDirectory + "/varFile-" + i + ".var"));
      Files.write(filePath, varFilesListContent.get(i).getBytes());
      varFilesList.add(filePath.toString());
    }
    return varFilesList;
  }
}
