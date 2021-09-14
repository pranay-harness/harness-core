/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.file;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ChecksumType;

import java.util.Map;

@OwnedBy(PL)
public interface GcsHarnessFileMetadata {
  String getFileName();
  String getGcsFileId();

  long getFileLength();
  void setFileLength(long fileLength);

  String getChecksum();
  void setChecksum(String checksum);
  ChecksumType getChecksumType();
  void setChecksumType(ChecksumType checksumType);

  String getMimeType();
  Map<String, Object> getOthers();
}
