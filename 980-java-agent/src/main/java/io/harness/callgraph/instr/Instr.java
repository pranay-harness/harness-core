package io.harness.callgraph.instr;

import java.lang.instrument.Instrumentation;
import java.util.Set;

public abstract class Instr {
  protected final Set<String> includes;

  protected Instr(Set<String> includes) {
    this.includes = includes;
  }

  public abstract void instrument(Instrumentation instrumentation);
}
