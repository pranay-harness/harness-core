package io.harness.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * The Class FileMetadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
