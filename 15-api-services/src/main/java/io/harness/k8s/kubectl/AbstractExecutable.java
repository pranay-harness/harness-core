package io.harness.k8s.kubectl;

import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class AbstractExecutable implements Executable {
  @Override
  public ProcessResult execute(String directory, OutputStream output, OutputStream error, boolean printCommand)
      throws Exception {
    String command = this.command();

    if (printCommand) {
      writeCommandToOutput(command, output);
    }

    return Utils.executeScript(directory, command, output, error);
  }

  @Override
  public StartedProcess executeInBackground(String directory, OutputStream output, OutputStream error)
      throws Exception {
    return Utils.startScript(directory, this.command(), output, error);
  }

  public static String getPrintableCommand(String command) {
    return command.substring(command.indexOf("kubectl --kubeconfig"));
  }

  private void writeCommandToOutput(String command, OutputStream output) throws Exception {
    String printCommand = "\n" + getPrintableCommand(command) + "\n\n";
    output.write(printCommand.getBytes(StandardCharsets.UTF_8));
  }
}