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
