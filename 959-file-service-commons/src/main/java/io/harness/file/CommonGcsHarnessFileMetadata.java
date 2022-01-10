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
