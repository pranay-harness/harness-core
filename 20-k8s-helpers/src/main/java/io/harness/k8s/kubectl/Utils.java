package io.harness.k8s.kubectl;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

import java.io.File;
import java.io.OutputStream;

public class Utils {
  private static final String newLineRegex = "\\r?\\n";

  public static ProcessResult executeScript(
      String directoryPath, String command, OutputStream output, OutputStream error) throws Exception {
    String[] commandList = new String[] {"Powershell", "-c", command};

    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .directory(new File(directoryPath))
                                          .command(commandList)
                                          .readOutput(true)
                                          .redirectOutput(output)
                                          .redirectError(error);

    return processExecutor.execute();
  }

  public static StartedProcess startScript(
      String directoryPath, String command, OutputStream output, OutputStream error) throws Exception {
    String[] commandList = new String[] {"Powershell", "-c", command};

    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .directory(new File(directoryPath))
                                          .command(commandList)
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
}
