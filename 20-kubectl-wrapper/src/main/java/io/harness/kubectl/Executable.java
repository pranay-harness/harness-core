package io.harness.kubectl;

import org.zeroturnaround.exec.ProcessResult;

import java.io.OutputStream;

interface Executable {
  ProcessResult execute(OutputStream output, OutputStream error) throws Exception;
  String command();
}