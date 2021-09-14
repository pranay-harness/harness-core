/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.file;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public interface HarnessFile {
  String getFileName();

  String getFileUuid();
  void setFileUuid(String fileUuid);

  String getChecksum();
  void setChecksum(String checksum);

  String getAccountId();
  String getMimeType();

  long getSize();
  void setSize(long size);
}
