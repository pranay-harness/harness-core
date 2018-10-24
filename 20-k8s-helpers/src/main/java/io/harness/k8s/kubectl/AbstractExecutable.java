package io.harness.k8s.kubectl;

import org.zeroturnaround.exec.ProcessResult;

import java.io.OutputStream;

public abstract class AbstractExecutable implements Executable {
  public ProcessResult execute(String directory, OutputStream output, OutputStream error) throws Exception {
    return Utils.executeScript(directory, this.command(), output, error);
  };
}