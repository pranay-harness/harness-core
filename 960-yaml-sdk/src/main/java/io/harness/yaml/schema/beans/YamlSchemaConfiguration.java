package io.harness.yaml.schema.beans;

import java.net.URLClassLoader;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class YamlSchemaConfiguration {
  /**
   * The root path where final json schema will be stored.
   */
  String generatedPathRoot;
  /**
   * Classloader which will be used for generation.
   */
  @Nullable URLClassLoader classLoader;
}
