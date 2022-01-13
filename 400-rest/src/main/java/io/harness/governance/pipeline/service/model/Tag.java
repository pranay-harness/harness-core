/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
