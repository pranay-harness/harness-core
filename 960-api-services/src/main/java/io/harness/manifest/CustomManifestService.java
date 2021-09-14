/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.manifest;

import io.harness.logging.LogCallback;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.validation.constraints.NotNull;

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
}
