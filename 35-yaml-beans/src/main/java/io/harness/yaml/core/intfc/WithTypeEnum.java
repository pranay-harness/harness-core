package io.harness.yaml.core.intfc;

public interface WithTypeEnum<T extends Enum<T>> {
  T getType();
}
