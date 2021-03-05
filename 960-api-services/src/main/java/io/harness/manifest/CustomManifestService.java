package io.harness.manifest;

import io.harness.logging.LogCallback;

import java.io.IOException;
import java.util.Collection;
import javax.validation.constraints.NotNull;

public interface CustomManifestService {
  void downloadCustomSource(@NotNull CustomManifestSource source, String outputDirectory, LogCallback logCallback)
      throws IOException;

  Collection<CustomSourceFile> fetchValues(@NotNull CustomManifestSource source, String workingDirectory,
      String activityId, LogCallback logCallback) throws IOException;

  String getWorkingDirectory() throws IOException;
}
