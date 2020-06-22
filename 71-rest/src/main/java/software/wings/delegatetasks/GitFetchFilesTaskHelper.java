package software.wings.delegatetasks;

import static java.lang.String.format;
import static software.wings.beans.Log.LogColor.Gray;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;

import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.yaml.GitFetchFilesResult;

import java.util.List;

@Singleton
public class GitFetchFilesTaskHelper {
  public void printFileNamesInExecutionLogs(
      GitFetchFilesResult gitFetchFilesResult, ExecutionLogCallback executionLogCallback) {
    if (gitFetchFilesResult == null || EmptyPredicate.isEmpty(gitFetchFilesResult.getFiles())) {
      return;
    }

    StringBuilder sb = new StringBuilder(1024);
    gitFetchFilesResult.getFiles().forEach(
        each -> sb.append(color(format("- %s", each.getFilePath()), Gray)).append(System.lineSeparator()));

    executionLogCallback.saveExecutionLog(color("Successfully fetched following files:", White, Bold));
    executionLogCallback.saveExecutionLog(sb.toString());
  }

  public void printFileNamesInExecutionLogs(List<String> filePathList, ExecutionLogCallback executionLogCallback) {
    if (EmptyPredicate.isEmpty(filePathList)) {
      return;
    }

    StringBuilder sb = new StringBuilder(1024);
    filePathList.forEach(filePath -> sb.append(color(format("- %s", filePath), Gray)).append(System.lineSeparator()));

    executionLogCallback.saveExecutionLog(sb.toString());
  }
}
