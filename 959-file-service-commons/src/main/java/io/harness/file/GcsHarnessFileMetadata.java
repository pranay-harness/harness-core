/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
