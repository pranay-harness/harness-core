package io.harness.exception;

import static io.harness.eraro.ErrorCode.READ_FILE_FROM_GCP_STORAGE_FAILED;

import io.harness.eraro.Level;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class GCPStorageFileReadException extends WingsException {
  public GCPStorageFileReadException(Throwable throwable) {
    super(null, throwable, READ_FILE_FROM_GCP_STORAGE_FAILED, Level.ERROR, null, null);
  }
}
