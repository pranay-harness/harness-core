/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.cv;

import static io.harness.eraro.ErrorCode.DATA_COLLECTION_ERROR;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class DataCollectionException extends WingsException {
  public DataCollectionException(Exception e) {
    super(e.getMessage(), e, DATA_COLLECTION_ERROR, Level.ERROR, null, null);
  }

  public DataCollectionException(String message) {
    super(message, null, DATA_COLLECTION_ERROR, Level.ERROR, null, null);
  }

  public DataCollectionException(String message, Exception e) {
    super(message, e, DATA_COLLECTION_ERROR, Level.ERROR, null, null);
  }
}
