package io.harness.mongo;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class IndexManagerReadOnlyException extends WingsException {
  public IndexManagerReadOnlyException() {
    super(null, null, GENERAL_ERROR, Level.ERROR, null, null);
  }
}
