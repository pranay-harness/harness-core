/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secrets;

import org.hibernate.validator.constraints.NotEmpty;

public interface SecretsFileService {
  String createFile(@NotEmpty String name, @NotEmpty String accountId, @NotEmpty char[] fileContent);
  char[] getFileContents(@NotEmpty String fileId);
  long getFileSizeLimit();
  void deleteFile(@NotEmpty char[] fileId);
}
