package io.harness.k8s.kubectl;

import org.zeroturnaround.exec.ProcessResult;

import java.io.OutputStream;

interface Executable {
  ProcessResult execute(String directory, OutputStream output, OutputStream error) throws Exception;
  String command();
}