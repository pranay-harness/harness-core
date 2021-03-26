package io.harness.delegate.task.terraform;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.provision.TerraformConstants.TERRAFORM_STATE_FILE_NAME;
import static io.harness.provision.TerraformConstants.WORKSPACE_STATE_FILE_PATH_FORMAT;

import static java.lang.String.format;

import io.harness.delegate.beans.DelegateFileManagerBase;
import io.harness.delegate.beans.FileBucket;

import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class TerraformBaseHelperImpl implements TerraformBaseHelper {
  @Inject private DelegateFileManagerBase delegateFileManagerBase;

  @Override
  public void downloadTfStateFile(String workspace, String accountId, String currentStateFileId, String scriptDirectory)
      throws IOException {
    File tfStateFile = (isEmpty(workspace))
        ? Paths.get(scriptDirectory, TERRAFORM_STATE_FILE_NAME).toFile()
        : Paths.get(scriptDirectory, format(WORKSPACE_STATE_FILE_PATH_FORMAT, workspace)).toFile();

    if (currentStateFileId != null) {
      try (InputStream stateRemoteInputStream =
               delegateFileManagerBase.downloadByFileId(FileBucket.TERRAFORM_STATE, currentStateFileId, accountId)) {
        FileUtils.copyInputStreamToFile(stateRemoteInputStream, tfStateFile);
      }
    } else {
      FileUtils.deleteQuietly(tfStateFile);
    }
  }

  @Override
  public List<String> parseOutput(String workspaceOutput) {
    List<String> outputs = Arrays.asList(StringUtils.split(workspaceOutput, "\n"));
    List<String> workspaces = new ArrayList<>();
    for (String output : outputs) {
      if (output.charAt(0) == '*') {
        output = output.substring(1);
      }
      output = output.trim();
      workspaces.add(output);
    }
    return workspaces;
  }
}
