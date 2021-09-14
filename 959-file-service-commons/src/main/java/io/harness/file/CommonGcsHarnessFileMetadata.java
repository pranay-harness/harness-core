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
import io.harness.delegate.beans.FileBucket;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(PL)
public class CommonGcsHarnessFileMetadata implements GcsHarnessFileMetadata {
  @NotEmpty private String accountId;
  @NotEmpty private String fileId; // Mongo GridFs fileId.
  @NotEmpty private String gcsFileId;
  @NotEmpty private String fileName;
  @NotEmpty private FileBucket fileBucket;
  private String entityId;
  private int version;
  private long fileLength;
  private String mimeType;
  private ChecksumType checksumType;
  private String checksum;
  private Map<String, Object> others;
}
