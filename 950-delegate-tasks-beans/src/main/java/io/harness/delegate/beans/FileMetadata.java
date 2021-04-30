package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

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
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
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
