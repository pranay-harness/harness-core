package io.harness.kubectl;

import org.zeroturnaround.exec.ProcessResult;

import java.io.OutputStream;

public abstract class AbstractExecutable implements Executable {
  public ProcessResult execute(OutputStream output, OutputStream error) throws Exception {
    return Utils.executeScript(this.command(), output, error);
  };
}