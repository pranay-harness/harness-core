package io.harness.yaml.core.defaults;

public interface DefaultStageProperties {
  String getName();
  boolean isRunParallel();
  String getSkipCondition();
  String getDescription();
}
