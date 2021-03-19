package io.harness.delegate.task.terraform;

import java.io.IOException;
import java.util.List;

public interface TerraformBaseHelper {
  void downloadTfStateFile(String workspace, String accountId, String currentStateFileId, String scriptDirectory)
      throws IOException;
  List<String> parseOutput(String workspaceOutput);
}
