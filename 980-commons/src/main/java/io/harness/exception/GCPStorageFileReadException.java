/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.READ_FILE_FROM_GCP_STORAGE_FAILED;

import io.harness.eraro.Level;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class GCPStorageFileReadException extends WingsException {
  public GCPStorageFileReadException(Throwable throwable) {
    super(null, throwable, READ_FILE_FROM_GCP_STORAGE_FAILED, Level.ERROR, null, null);
  }
}
