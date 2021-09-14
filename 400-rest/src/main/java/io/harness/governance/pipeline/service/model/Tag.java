/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.governance.pipeline.service.model;

import software.wings.beans.HarnessTagLink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag {
  @Nonnull private String key;
  @Nullable private String value;

  public static Tag fromTagLink(HarnessTagLink tagLink) {
    return new Tag(tagLink.getKey(), tagLink.getValue());
  }
}
