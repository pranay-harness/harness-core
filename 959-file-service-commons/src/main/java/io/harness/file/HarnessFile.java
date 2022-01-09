/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
