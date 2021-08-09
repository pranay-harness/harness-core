package io.harness.k8s.kubectl;

import java.io.OutputStream;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

interface Executable {
  ProcessResult execute(String directory, OutputStream output, OutputStream error, boolean printCommand)
      throws Exception;
  StartedProcess executeInBackground(String directory, OutputStream output, OutputStream error) throws Exception;
  String command();
}
