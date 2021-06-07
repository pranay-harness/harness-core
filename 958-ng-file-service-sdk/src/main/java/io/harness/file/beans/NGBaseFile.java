package io.harness.file.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ChecksumType;
import io.harness.file.HarnessFile;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NGBaseFile implements HarnessFile {
  String fileUuid;
  String fileName;
  String accountId;
  ChecksumType checksumType = ChecksumType.MD5;
  String checksum;
  String mimeType;
  long size;
}
