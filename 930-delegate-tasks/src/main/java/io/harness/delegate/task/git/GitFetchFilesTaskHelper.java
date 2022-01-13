/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.git;

import static software.wings.beans.LogColor.Gray;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;

import io.harness.data.structure.EmptyPredicate;
import io.harness.git.model.GitFile;
import io.harness.logging.LogCallback;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class GitFetchFilesTaskHelper {
  public void printFileNamesInExecutionLogs(LogCallback executionLogCallback, List<GitFile> files) {
    if (EmptyPredicate.isEmpty(files)) {
      return;
    }

    StringBuilder sb = new StringBuilder(1024);
    files.forEach(each -> sb.append(color(format("- %s", each.getFilePath()), Gray)).append(System.lineSeparator()));

    executionLogCallback.saveExecutionLog(color("Successfully fetched following files:", White, Bold));
    executionLogCallback.saveExecutionLog(sb.toString());
  }

  public void printFileNamesInExecutionLogs(List<String> filePathList, LogCallback logCallback) {
    if (EmptyPredicate.isEmpty(filePathList)) {
      return;
    }

    StringBuilder sb = new StringBuilder(1024);
    filePathList.forEach(filePath -> sb.append(color(format("- %s", filePath), Gray)).append(System.lineSeparator()));

    logCallback.saveExecutionLog(sb.toString());
  }
}
