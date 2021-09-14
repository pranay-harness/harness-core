/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.manifest;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class CustomSourceConfig {
  @Nullable private String script;
  private String path;
  private List<String> delegateSelectors;

  @Nullable
  public static CustomSourceConfig cloneFrom(@Nullable CustomSourceConfig sourceConfig) {
    if (sourceConfig == null) {
      return null;
    }

    return CustomSourceConfig.builder()
        .script(sourceConfig.getScript())
        .path(sourceConfig.getPath())
        .delegateSelectors(sourceConfig.getDelegateSelectors())
        .build();
  }
}
