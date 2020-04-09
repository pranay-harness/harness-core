package io.harness.k8s.kubectl;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

import java.io.File;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class Utils {
  private static final String newLineRegex = "\\r?\\n";

  public static ProcessResult executeScript(
      String directoryPath, String command, OutputStream output, OutputStream error) throws Exception {
    ProcessExecutor processExecutor = new ProcessExecutor()

                                          .directory(new File(directoryPath))
                                          .commandSplit(command)
                                          .readOutput(true)
                                          .redirectOutput(output)
                                          .redirectError(error);

    return processExecutor.execute();
  }

  public static StartedProcess startScript(
      String directoryPath, String command, OutputStream output, OutputStream error) throws Exception {
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .directory(new File(directoryPath))
                                          .commandSplit(command)
                                          .readOutput(true)
                                          .redirectOutput(output)
                                          .redirectError(error);

    return processExecutor.start();
  }

  public static String parseLatestRevisionNumberFromRolloutHistory(String rolloutHistory) {
    // assumes valid input from `kubectl rollout history`
    String[] lines = rolloutHistory.split(newLineRegex);
    return lines[lines.length - 1].split(" ")[0];
  }

  public static String encloseWithQuotesIfNeeded(String path) {
    String result = path.trim();
    if (StringUtils.containsWhitespace(result)) {
      result = "\"" + result + "\"";
    }
    return result;
  }

  public static boolean executeCommand(String command, int timeoutMinutes) {
    try {
      ProcessExecutor processExecutor =
          new ProcessExecutor().timeout(timeoutMinutes, TimeUnit.MINUTES).commandSplit(command).readOutput(true);
      ProcessResult processResult = processExecutor.execute();
      return processResult.getExitValue() == 0;
    } catch (Exception ex) {
      return false;
    }
  }
}
