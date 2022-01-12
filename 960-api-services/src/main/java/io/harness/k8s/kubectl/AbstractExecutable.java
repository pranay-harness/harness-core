/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
