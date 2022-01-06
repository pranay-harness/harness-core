/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.cv;

import static io.harness.eraro.ErrorCode.DATA_COLLECTION_ERROR;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(HarnessTeam.CV)
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
