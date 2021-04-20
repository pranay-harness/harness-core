package harness.callgraph.instr;

import java.lang.instrument.Instrumentation;
import java.util.Set;

public abstract class Instr {
  protected final Set<String> includes;
  protected final Set<String> testAnnotations;

  protected Instr(Set<String> includes, Set<String> testAnnotations) {
    this.includes = includes;
    this.testAnnotations = testAnnotations;
  }

  public abstract void instrument(Instrumentation instrumentation);
}
