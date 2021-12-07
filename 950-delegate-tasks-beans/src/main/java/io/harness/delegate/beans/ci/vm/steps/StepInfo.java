package io.harness.delegate.beans.ci.vm.steps;

public interface StepInfo {
  enum Type { RUN, PLUGIN, RUN_TEST }
  Type getType();
}
