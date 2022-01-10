/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface DelegateFileManagerBase {
  DelegateFile upload(DelegateFile delegateFile, InputStream contentSource);
  String getFileIdByVersion(FileBucket fileBucket, String entityId, int version, String accountId) throws IOException;
  InputStream downloadByFileId(@NotNull FileBucket bucket, @NotEmpty String fileId, @NotEmpty String accountId)
      throws IOException;
  InputStream downloadByConfigFileId(String fileId, String accountId, String appId, String activityId)
      throws IOException;
  DelegateFile getMetaInfo(FileBucket fileBucket, String fileId, String accountId) throws IOException;
  DelegateFile uploadAsFile(DelegateFile delegateFile, File File);
}
