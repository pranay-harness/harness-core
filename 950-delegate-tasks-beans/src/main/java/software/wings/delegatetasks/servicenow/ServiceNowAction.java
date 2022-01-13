/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.servicenow;

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
