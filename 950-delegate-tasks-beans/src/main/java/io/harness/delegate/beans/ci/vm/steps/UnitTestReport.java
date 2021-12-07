package io.harness.delegate.beans.ci.vm.steps;

public interface UnitTestReport {
  enum Type { JUNIT }
  Type getType();
}
