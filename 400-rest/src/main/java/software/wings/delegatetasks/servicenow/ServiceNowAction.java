/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.delegatetasks.servicenow;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public enum ServiceNowAction {
  CREATE("Create"),
  UPDATE("Update"),
  IMPORT_SET("Import Set");

  private String displayName;
  ServiceNowAction(String s) {
    displayName = s;
  }

  public String getDisplayName() {
    return displayName;
  }
}
