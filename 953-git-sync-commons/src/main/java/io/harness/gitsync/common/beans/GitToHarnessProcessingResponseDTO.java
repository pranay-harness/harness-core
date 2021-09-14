/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.beans;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "ProcessingResponseKeys")
public class GitToHarnessProcessingResponseDTO {
  List<FileProcessingResponseDTO> fileResponses;
  String accountId;
  MsvcProcessingFailureStage msvcProcessingFailureStage;
}
