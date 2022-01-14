package io.harness.cli;

import org.zeroturnaround.exec.stream.LogOutputStream;

public class EmptyLogOutputStream extends LogOutputStream {
  @Override
  protected void processLine(String s) {
    // Not Logging so that secrets are not exposed
  }
}
