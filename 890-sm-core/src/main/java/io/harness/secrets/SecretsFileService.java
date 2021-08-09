package io.harness.secrets;

import org.hibernate.validator.constraints.NotEmpty;

public interface SecretsFileService {
  String createFile(@NotEmpty String name, @NotEmpty String accountId, @NotEmpty char[] fileContent);
  char[] getFileContents(@NotEmpty String fileId);
  long getFileSizeLimit();
  void deleteFile(@NotEmpty char[] fileId);
}
