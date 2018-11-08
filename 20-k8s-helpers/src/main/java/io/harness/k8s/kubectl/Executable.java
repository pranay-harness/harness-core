package io.harness.k8s.kubectl;

import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

import java.io.OutputStream;

interface Executable {
  ProcessResult execute(String directory, OutputStream output, OutputStream error) throws Exception;
  StartedProcess executeInBackground(String directory, OutputStream output, OutputStream error) throws Exception;
  String command();
}