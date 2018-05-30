package software.wings.beans;

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
public class FileMetadata {
  private String fileName;
  private String mimeType;
  private ChecksumType checksumType;
  private String checksum;
  private String relativePath;
}
