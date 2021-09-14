/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.app.beans.entities;

import io.harness.ng.core.dashboard.AuthorInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildFailureInfo {
  private String piplineName;
  private String pipelineIdentifier;
  private String branch;
  private String commit;
  private String commitID;
  private AuthorInfo author;
  private long startTs;
  private long endTs;
  String status;
}
