/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class FileContentBatchResponse {
  FileBatchContentResponse fileBatchContentResponse;
  String commitId;
}
