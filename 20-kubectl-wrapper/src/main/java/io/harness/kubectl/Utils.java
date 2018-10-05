package io.harness.kubectl;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.OutputStream;

public class Utils {
  public static ProcessResult executeScript(String command, OutputStream output, OutputStream error) throws Exception {
    String[] commandList = new String[] {"Powershell", "-c", command};

    ProcessExecutor processExecutor =
        new ProcessExecutor().command(commandList).readOutput(true).redirectOutput(output).redirectError(error);

    return processExecutor.execute();
  }
}
