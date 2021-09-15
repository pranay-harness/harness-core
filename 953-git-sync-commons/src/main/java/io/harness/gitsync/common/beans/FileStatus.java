/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.beans;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "FileStatusKeys")
@ToString(exclude = "fileContent")
public class FileStatus {
  String filePath;
  String fileContent;
  String changeType;
  String entityType;
  FileProcessStatus status;
  String errorMessage;
}
