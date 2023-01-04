/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.terragrunt.files;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.logging.LogCallback;

import java.io.IOException;

@OwnedBy(CDP)
public interface FileStoreDownloadService {
  DownloadResult download(StoreDelegateConfig storeConfig, String accountId, String outputDirectory,
      LogCallback logCallback) throws IOException;

  FetchFilesResult fetchFiles(StoreDelegateConfig storeConfig, String accountId, String outputDirectory,
      LogCallback logCallback) throws IOException;
}
