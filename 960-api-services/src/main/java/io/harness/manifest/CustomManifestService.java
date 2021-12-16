package io.harness.manifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.LogCallback;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.CDP)
public interface CustomManifestService {
  void downloadCustomSource(@NotNull CustomManifestSource source, String outputDirectory, LogCallback logCallback)
      throws IOException;

  Collection<CustomSourceFile> fetchValues(@NotNull CustomManifestSource source, String workingDirectory,
      String activityId, LogCallback logCallback) throws IOException;

  String getWorkingDirectory() throws IOException;

  @NotNull
  String executeCustomSourceScript(
      String activityId, LogCallback logCallback, CustomManifestSource customManifestSource) throws IOException;

  Collection<CustomSourceFile> readFilesContent(String parentDirectory, List<String> filesPath) throws IOException;

  void cleanup(String path);
}
