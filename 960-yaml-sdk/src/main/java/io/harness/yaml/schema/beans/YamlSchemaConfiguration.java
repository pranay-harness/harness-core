/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.schema.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.net.URLClassLoader;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class YamlSchemaConfiguration {
  /**
   * The root path where final json schema will be stored.
   */
  @Nullable String generatedPathRoot;
  /**
   * Classloader which will be used for generation.
   */
  @Nullable URLClassLoader classLoader;

  boolean generateFiles;
  @Default boolean generateOnlyRootFile = true;
}
