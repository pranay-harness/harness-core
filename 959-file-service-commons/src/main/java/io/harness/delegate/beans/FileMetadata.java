/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The Class FileMetadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(HarnessTeam.DEL)
public class FileMetadata {
  private String fileUuid;
  private String fileName;
  private long fileLength;
  private String accountId;
  private String mimeType;
  private ChecksumType checksumType;
  private String checksum;
  private String relativePath;
  // Additional metadata stored as a map.
  private Map<String, Object> metadata;
}
