package io.harness.callgraph.instr;

import java.lang.instrument.Instrumentation;
import java.util.List;

public abstract class Instr {
  protected final List<String> includes;

  protected Instr(List<String> includes) {
    this.includes = includes;
  }

  public abstract void instrument(Instrumentation instrumentation);
}
