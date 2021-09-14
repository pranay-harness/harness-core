/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.templates.google;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.GTM)
@Value
@Builder
public class DownloadResult {
  private String fileName;
  private Long updateTime;
  private byte[] content;

  public static final DownloadResult NULL_RESULT = new DownloadResult(null, null, null);
}
