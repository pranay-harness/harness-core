/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.yaml.extended;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ImagePullPolicy {
  @JsonProperty("Always") ALWAYS("Always"),
  @JsonProperty("Never") NEVER("Never"),
  @JsonProperty("IfNotPresent") If_NOT_PRESENT("IfNotPresent");
  private final String yamlName;

  @JsonCreator
  public static ImagePullPolicy getImagePullPolicy(@JsonProperty("pullPolicy") String yamlName) {
    for (ImagePullPolicy pullPolicy : ImagePullPolicy.values()) {
      if (pullPolicy.yamlName.equalsIgnoreCase(yamlName)) {
        return pullPolicy;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + yamlName);
  }

  ImagePullPolicy(String yamlName) {
    this.yamlName = yamlName;
  }

  @JsonValue
  public String getYamlName() {
    return yamlName;
  }

  @Override
  public String toString() {
    return yamlName;
  }

  public static ImagePullPolicy fromString(final String s) {
    return ImagePullPolicy.getImagePullPolicy(s);
  }
}
