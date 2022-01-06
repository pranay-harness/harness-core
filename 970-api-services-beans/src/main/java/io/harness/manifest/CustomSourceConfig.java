/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
