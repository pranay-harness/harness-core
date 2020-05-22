package io.harness.yaml.core.deserializer;

public interface TypeDescriptor {
  /**
   * @return type handled by deserializer
   */
  Class<?> getType();

  /**
   * @return name of the property that holds type information.
   */
  String getTypePropertyName();
}
