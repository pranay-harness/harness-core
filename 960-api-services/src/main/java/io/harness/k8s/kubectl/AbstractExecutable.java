/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.kubectl;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

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
    int index = command.indexOf("kubectl --kubeconfig");
    if (index != -1) {
      return command.substring(index);
    }

    return command.substring(command.indexOf("oc --kubeconfig"));
  }

  private void writeCommandToOutput(String command, OutputStream output) throws Exception {
    String printCommand = "\n" + getPrintableCommand(command) + "\n\n";
    output.write(printCommand.getBytes(StandardCharsets.UTF_8));
  }
}
