package io.harness.delegate.task.git;

import static java.lang.String.format;
import static software.wings.beans.LogColor.Gray;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.git.model.GitFile;
import io.harness.logging.LogCallback;

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
